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

export async function updateClassSession(id, schedule) {
  const response = await api.put(
    `${BASE_URL}/${id}/schedule`,
    schedule
  );
  return response.data;
}
