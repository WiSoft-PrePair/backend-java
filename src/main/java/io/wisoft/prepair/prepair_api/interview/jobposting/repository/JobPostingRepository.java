package io.wisoft.prepair.prepair_api.interview.jobposting.repository;

import io.wisoft.prepair.prepair_api.interview.jobposting.entity.JobPosting;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface JobPostingRepository extends JpaRepository<JobPosting, UUID> {

    Optional<JobPosting> findBySourceUrl(String sourceUrl);
}
