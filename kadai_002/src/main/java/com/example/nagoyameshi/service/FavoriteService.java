package com.example.nagoyameshi.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.nagoyameshi.entity.Favorite;
import com.example.nagoyameshi.entity.Member;
import com.example.nagoyameshi.entity.Store;
import com.example.nagoyameshi.repository.FavoriteRepository;
import com.example.nagoyameshi.repository.StoreRepository;

@Service
public class FavoriteService {

    @Autowired
    private FavoriteRepository favoriteRepository;

    @Autowired
    private StoreRepository storeRepository;

    @Transactional
    public void addFavorite(Member member, Long storeId) {
        Store store = storeRepository.findById(storeId)
                .orElseThrow(() -> new IllegalArgumentException("店舗が見つかりません。"));
        if (!favoriteRepository.existsByMemberAndStore(member, store)) {
            Favorite favorite = new Favorite();
            favorite.setMember(member);
            favorite.setStore(store);
            favoriteRepository.save(favorite);
        }
    }

    @Transactional
    public void removeFavorite(Member member, Long storeId) {
        Store store = storeRepository.findById(storeId)
                .orElseThrow(() -> new IllegalArgumentException("店舗が見つかりません。"));
        favoriteRepository.deleteByMemberAndStore(member, store);
    }

    @Transactional(readOnly = true)
    public boolean isFavorited(Member member, Long storeId) {
        Store store = storeRepository.findById(storeId)
                .orElseThrow(() -> new IllegalArgumentException("店舗が見つかりません。"));
        return favoriteRepository.existsByMemberAndStore(member, store);
    }

    @Transactional(readOnly = true)
    public Page<Favorite> findFavoritesByMember(Member member, Pageable pageable) {
        Page<Favorite> favorites = favoriteRepository.findByMember(member, pageable);

        // 各Favoriteエンティティに`isFavorited`フラグを設定
        favorites.forEach(favorite -> favorite.setFavorited(true));

        return favorites;
    }

}
