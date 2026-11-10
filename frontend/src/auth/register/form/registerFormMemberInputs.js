import { formValidators } from "../../../validators/formValidators";

export const registerFormMemberInputs = [
  "username", "password", "firstName", "lastName", "city", "email", "telephone"
].map((name) => ({
  tag: name.replace(/([A-Z])/g, " $1").replace(/^./, (value) => value.toUpperCase()),
  name,
  type: name === "password" ? "password" : "text",
  defaultValue: "",
  isRequired: !["telephone"].includes(name),
  validators: !["telephone"].includes(name) ? [formValidators.notEmptyValidator] : [],
}));
