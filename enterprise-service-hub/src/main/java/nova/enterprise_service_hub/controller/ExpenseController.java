package nova.enterprise_service_hub.controller;

import jakarta.validation.Valid;
import nova.enterprise_service_hub.dto.ExpenseDTO;
import nova.enterprise_service_hub.dto.PageResponse;
import nova.enterprise_service_hub.service.ExpenseService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Controller for managing Expenses.
 * Allowed for ADMIN and USER roles.
 */
@RestController
@RequestMapping("/v1/expenses")
@PreAuthorize("hasAnyRole('ADMIN', 'USER')")
public class ExpenseController {

    private final ExpenseService expenseService;

    public ExpenseController(ExpenseService expenseService) {
        this.expenseService = expenseService;
    }

    @GetMapping
    public ResponseEntity<PageResponse<ExpenseDTO>> getAllExpenses(
            @PageableDefault(size = 20, sort = "expenseDate", direction = Sort.Direction.DESC) Pageable pageable) {
        Page<ExpenseDTO> page = expenseService.findAllPaged(pageable);
        return ResponseEntity.ok(new PageResponse<>(
                page.getContent(),
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages(),
                page.isLast()
        ));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ExpenseDTO> getExpenseById(@PathVariable Long id) {
        return ResponseEntity.ok(expenseService.findById(id));
    }

    @PostMapping
    public ResponseEntity<ExpenseDTO> createExpense(@Valid @RequestBody ExpenseDTO expenseDTO) {
        return ResponseEntity.status(HttpStatus.CREATED).body(expenseService.create(expenseDTO));
    }
}
