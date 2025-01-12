package com.example.nagoyameshi.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.transaction.annotation.Transactional;

import com.example.nagoyameshi.entity.Member;
import com.example.nagoyameshi.entity.Reservation;
import com.example.nagoyameshi.entity.Store;

public interface ReservationRepository extends JpaRepository<Reservation, Long> {
	public Page<Reservation> findByMemberOrderByReservationIdDesc(Member member, Pageable pageable);
	
	 Page<Reservation> findByStore(Store store, Pageable pageable);
	 
	 // 店舗削除用
	 @Modifying
	    @Transactional
	    @Query("DELETE FROM Reservation r WHERE r.store.storeId = :storeId")
	    void deleteByStoreId(Long storeId);
}