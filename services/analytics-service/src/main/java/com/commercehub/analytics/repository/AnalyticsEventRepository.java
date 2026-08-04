package com.commercehub.analytics.repository;

import com.commercehub.analytics.entity.AnalyticsEvent;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface AnalyticsEventRepository extends JpaRepository<AnalyticsEvent, UUID> {

    Page<AnalyticsEvent> findAllByOrderByReceivedAtDesc(Pageable pageable);

    long countByEventType(String eventType);
}
