import { formValidators } from "../../../validators/formValidators";
import { registerFormGymOwnerInputs } from "./registerFormGymOwnerInputs";

export const registerFormInstructorInputs = [
  ...registerFormGymOwnerInputs,
  {
    tag: "City",
    name: "city",
    type: "text",
    defaultValue: "",
    isRequired: true,
    validators: [formValidators.notEmptyValidator],
  },
  {
    tag: "Gym",
    name: "gym",
    type: "select",
    values: ["None"],
    defaultValue: "",
    isRequired: true,
    validators: [formValidators.notEmptyValidator, formValidators.notNoneTypeValidator],
  },
];
