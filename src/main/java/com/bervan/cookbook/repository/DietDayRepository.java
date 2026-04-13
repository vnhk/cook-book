package com.bervan.cookbook.repository;

import com.bervan.cookbook.model.DietDay;
import com.bervan.history.model.BaseRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface DietDayRepository extends BaseRepository<DietDay, UUID> {
    Optional<DietDay> findByDateAndDeletedFalse(LocalDate date);
    List<DietDay> findByDeletedFalseOrderByDateDesc();
    List<DietDay> findByDateBetweenAndDeletedFalseOrderByDate(LocalDate from, LocalDate to);
}
