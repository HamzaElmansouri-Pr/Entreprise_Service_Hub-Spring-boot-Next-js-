package nova.enterprise_service_hub.repository;

import nova.enterprise_service_hub.model.Proposal;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ProposalRepository extends JpaRepository<Proposal, Long> {

    List<Proposal> findByTenantIdOrderByCreatedAtDesc(String tenantId);

    Page<Proposal> findByTenantId(String tenantId, Pageable pageable);

    Optional<Proposal> findByIdAndTenantId(Long id, String tenantId);

    Optional<Proposal> findBySigningToken(String signingToken);

    Optional<Proposal> findByProposalNumber(String proposalNumber);

    List<Proposal> findByLeadId(Long leadId);

    long countByTenantId(String tenantId);

    long countByTenantIdAndStatus(String tenantId, Proposal.ProposalStatus status);
}
