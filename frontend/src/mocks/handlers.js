import { http, HttpResponse } from "msw";

const classEntities = [
  { id: 1, name: "Leo", birthDate: "2020-01-01" },
  { id: 2, name: "Basil", birthDate: "2020-02-01" }
];

const instructors = [
  { id: 1, firstName: "instructor1", lastName: "", city: "Madrid", gym: { name: "Gym" }, user: { username: "user1" } },
  { id: 2, firstName: "instructor2", lastName: "", city: "Madrid", gym: { name: "Gym" }, user: { username: "user2" } }
];

const users = [
  { id: 1, username: "owner1", authority: { authority: "GYM_OWNER" } },
  { id: 2, username: "admin1", authority: { authority: "ADMIN" } }
];

const membershipTiers = [
  {
    id: 1,
    name: "Standard",
    price: 30,
    durationDays: 30,
    entryQuota: null,
    allowedServices: ["WEIGHT_ROOM"],
    timeBand: "FULL_DAY"
  }
];

function deleteHandler() {
  return HttpResponse.json({ message: "Deleted successfully" });
}

export const handlers = [
  http.get("*/api/v1/classes", () => HttpResponse.json(classEntities)),
  http.delete("*/api/v1/classes/:id", deleteHandler),
  http.get("*/api/v1/instructors", () => HttpResponse.json(instructors)),
  http.delete("*/api/v1/instructors/:id", deleteHandler),
  http.get("*/api/v1/users", () => HttpResponse.json(users)),
  http.delete("*/api/v1/users/:id", deleteHandler),
  http.get("*/api/membership-tiers", () => HttpResponse.json(membershipTiers)),
  http.get("*/api/membership-tiers/:id/services", () => HttpResponse.json(["WEIGHT_ROOM"])),
  http.get("*/api/membership-tiers/:id/time-band", () => HttpResponse.json("FULL_DAY"))
];
