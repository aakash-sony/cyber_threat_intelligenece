package com.cyberthreat.dashboard.repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.cyberthreat.dashboard.entity.IpGeolocationEntity;

@Repository
public interface IpGeolocationRepository extends JpaRepository<IpGeolocationEntity, Long> {

	Optional<IpGeolocationEntity> findByIp(String ip);

	List<IpGeolocationEntity> findByIpIn(Collection<String> ips);
}
