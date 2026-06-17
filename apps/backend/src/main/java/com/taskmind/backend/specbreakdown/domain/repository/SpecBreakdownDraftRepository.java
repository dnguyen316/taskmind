package com.taskmind.backend.specbreakdown.domain.repository;
import com.taskmind.backend.specbreakdown.domain.model.SpecBreakdownDraft; import java.util.*;
public interface SpecBreakdownDraftRepository { SpecBreakdownDraft save(SpecBreakdownDraft draft); Optional<SpecBreakdownDraft> findById(UUID id); List<SpecBreakdownDraft> findByProjectId(UUID projectId); }
