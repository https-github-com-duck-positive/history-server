package com.UMC.history.repository;

import com.UMC.history.DTO.CommonDTO;
import com.UMC.history.entity.strongEntity.PostEntity;
import com.UMC.history.entity.strongEntity.UserEntity;
import com.UMC.history.entity.weekEntity.LikeEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface LikeRepository extends JpaRepository<LikeEntity, Long> {
    //    List<CommonDTO.LikeUserProtected> findByUser(UserEntity user);
    LikeEntity findByPostAndUser(PostEntity postEntity, UserEntity userEntity);

    //    List<CommonDTO.LikeUserProtected> findByUserOrderByCreatedDateDesc(UserEntity userEntity);
    List<LikeEntity> findByUserOrderByCreatedDateDesc(UserEntity userEntity);
}