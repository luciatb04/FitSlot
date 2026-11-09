package org.springframework.samples.fitslot.gym;

import java.util.List;
import java.util.Optional;

import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.samples.fitslot.instructor.Instructor;
import org.springframework.samples.fitslot.owner.Owner;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class GymService {
    private GymRepository gymRepository;

    @Autowired
	public GymService(GymRepository gymRepository) {
		this.gymRepository = gymRepository;
	}

	@Transactional(readOnly = true)
	public List<Gym> findAll() throws DataAccessException {
		
		return (List<Gym>) gymRepository.findAll();
	}

	@Transactional(readOnly = true)
	public List<Gym> findGymsByUserId(int userId) throws DataAccessException {
		return gymRepository.findGymsByUserId(userId);
	}

	@Transactional(readOnly = true)
	public Gym findGymById(int gymId) throws DataAccessException {
		
		Optional<Gym> gym = gymRepository.findById(gymId);
		
		if(gym.isPresent()) {
			return gym.get();
		}else{
			return null;
		}
	}

	@Transactional(readOnly = true)
	public List<Owner> findOwnersOfUserGyms(int userId) throws DataAccessException {
		return gymRepository.findOwnersOfUserGyms(userId);
	}

	@Transactional(readOnly = true)
	public List<Instructor> findInstructorsOfUserGyms(int userId) throws DataAccessException {
		return gymRepository.findInstructorsOfUserGyms(userId);
	}

    @Transactional
	public Gym save(Gym gym) throws DataAccessException {
		gymRepository.save(gym);
		return gym;
	}

	@Transactional
	public Gym update(Gym gym, int gymId) throws DataAccessException {
		
		Gym gymToUpdate = gymRepository.findById(gymId).get();
		if (gym.getGymOwner() != null){
			BeanUtils.copyProperties(gym, gymToUpdate, "id", "owners");
		}else{
			BeanUtils.copyProperties(gym, gymToUpdate, "id", "gymOwner", "owners");
		}

		return save(gymToUpdate);
	}

	@Transactional
	public void delete(int gymId) throws DataAccessException {
		gymRepository.deleteById(gymId);
	}
}
