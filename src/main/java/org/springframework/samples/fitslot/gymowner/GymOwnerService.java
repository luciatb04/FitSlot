package org.springframework.samples.fitslot.gymowner;

import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class GymOwnerService {
    private GymOwnerRepository gymOwnerRepository;

    @Autowired
	public GymOwnerService(GymOwnerRepository gymOwnerRepository) {
		this.gymOwnerRepository = gymOwnerRepository;
	}

	@Transactional(readOnly = true)
	public Iterable<GymOwner> findAll() throws DataAccessException {
		return gymOwnerRepository.findAll();
	}

	@Transactional(readOnly = true)
	public GymOwner findById(int gymOwnerId) throws DataAccessException {
		return gymOwnerRepository.findById(gymOwnerId).get();
	}

	@Transactional(readOnly = true)
	public GymOwner findByUserId(int userId) throws DataAccessException {
		
		Optional<GymOwner> gymOwner = gymOwnerRepository.findByUserId(userId);
		
		if(gymOwner.isPresent()) {
			return gymOwner.get();
		}else{
			return null;
		}
	}

    @Transactional
	public GymOwner saveGymOwner(GymOwner gymOwner) throws DataAccessException {
		gymOwnerRepository.save(gymOwner);
		return gymOwner;
	}

	@Transactional
	public void deleteById(int gymOwnerId) throws DataAccessException {
		gymOwnerRepository.deleteById(gymOwnerId);
	}
}
