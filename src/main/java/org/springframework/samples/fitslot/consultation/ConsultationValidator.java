package org.springframework.samples.fitslot.consultation;

import org.springframework.samples.fitslot.owner.Owner;
import org.springframework.samples.fitslot.classes.Classes;
import org.springframework.validation.Errors;
import org.springframework.validation.Validator;

public class ConsultationValidator implements Validator {

	@Override
	public void validate(Object obj, Errors errors) {
		Consultation consultation = (Consultation) obj;
		
		// class validation
		Classes classEntity = consultation.getClasses();
		Owner owner = consultation.getOwner();
		if (!classEntity.getOwner().getId().equals(owner.getId())) {
			errors.rejectValue("Classes", "Classes does not belong to the selected owner", "Classes does not belong to the selected owner");
		}			
		
	}

	/**
	 * This Validator validates *just* Consultation instances
	 */
	@Override
	public boolean supports(Class<?> clazz) {
		return Consultation.class.isAssignableFrom(clazz);
	}

}
