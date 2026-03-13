package nova.enterprise_service_hub.service;

import nova.enterprise_service_hub.dto.ExpenseDTO;
import nova.enterprise_service_hub.model.Expense;
import nova.enterprise_service_hub.repository.ExpenseRepository;
import nova.enterprise_service_hub.security.TenantContext;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
public class ExpenseService {

    private final ExpenseRepository expenseRepository;

    public ExpenseService(ExpenseRepository expenseRepository) {
        this.expenseRepository = expenseRepository;
    }

    @Transactional(readOnly = true)
    public List<ExpenseDTO> findAll() {
        return expenseRepository.findAll().stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public Page<ExpenseDTO> findAllPaged(Pageable pageable) {
        return expenseRepository.findAll(pageable).map(this::toDTO);
    }
    
    @Transactional(readOnly = true)
    public ExpenseDTO findById(Long id) {
        Expense expense = expenseRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Expense not found"));
        // Tenant check is handled by @Filter usually, but good to be safe if not active
        if (!expense.getTenantId().equals(TenantContext.getTenantId())) {
             throw new AccessDeniedException("Access denied");
        }
        return toDTO(expense);
    }

    public ExpenseDTO create(ExpenseDTO request) {
        Expense expense = new Expense();
        updateEntityFromDTO(expense, request);
        expense.setTenantId(TenantContext.getTenantId());
        
        Expense saved = expenseRepository.save(expense);
        return toDTO(saved);
    }

    private void updateEntityFromDTO(Expense entity, ExpenseDTO dto) {
        entity.setDescription(dto.description());
        entity.setAmount(dto.amount());
        if (dto.category() != null) {
            try {
                entity.setCategory(Expense.ExpenseCategory.valueOf(dto.category()));
            } catch (IllegalArgumentException e) {
                entity.setCategory(Expense.ExpenseCategory.OTHER);
            }
        }
        entity.setVendor(dto.vendor());
        entity.setExpenseDate(dto.expenseDate());
        entity.setReferenceNumber(dto.referenceNumber());
        entity.setNotes(dto.notes());
        entity.setRecurring(dto.recurring());
    }

    private ExpenseDTO toDTO(Expense expense) {
        return new ExpenseDTO(
                expense.getId(),
                expense.getDescription(),
                expense.getAmount(),
                expense.getCategory().name(),
                expense.getVendor(),
                expense.getExpenseDate(),
                expense.getReferenceNumber(),
                expense.getNotes(),
                expense.isRecurring(),
                expense.getCreatedAt(),
                expense.getUpdatedAt()
        );
    }
}
