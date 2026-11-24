import api from "./api";

const BASE_URL = "/class-sessions";

export async function getClassSessions() {
  const response = await api.get(BASE_URL);
  return response.data;
}

export async function getClassSession(id) {
  const response = await api.get(`${BASE_URL}/${id}`);
  return response.data;
}

export async function createClassSession(classSession) {
  const response = await api.post(BASE_URL, classSession);
  return response.data;
}

export async function updateClassSession(id, schedule) {
  const response = await api.put(
    `${BASE_URL}/${id}/schedule`,
    schedule
  );
  return response.data;
}

export async function updateClassCapacity(id, capacity) {
  const response = await api.put(
    `${BASE_URL}/${id}/capacity`,
    capacity
  );
  return response.data;
}

export async function deleteClassSession(id) {
  const response = await api.delete(`${BASE_URL}/${id}`);
  return response.data;
}
