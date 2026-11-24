import { fireEvent, render, screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { http, HttpResponse } from "msw";
import { MemoryRouter, Route, Routes } from "react-router-dom";
import { vi } from "vitest";
import { server } from "../../mocks/server";
import tokenService from "../../services/token.service";
import CreateClassSession from "./CreateClassSession";

function renderPage() {
  return render(
    <MemoryRouter initialEntries={["/manage-classes/new"]}>
      <Routes>
        <Route
          path="/manage-classes/new"
          element={<CreateClassSession />}
        />
        <Route
          path="/manage-classes"
          element={<h1>Class Management</h1>}
        />
      </Routes>
    </MemoryRouter>
  );
}

async function fillRequiredFields(user) {
  await user.type(screen.getByLabelText("Class name"), "Yoga");
  await user.selectOptions(screen.getByLabelText("Gym"), "7");
  fireEvent.change(screen.getByLabelText("Class date"), {
    target: { value: "2026-12-01" }
  });
  await user.selectOptions(
    screen.getByLabelText("Room"),
    "CLASS_ROOM_1"
  );
  await user.type(screen.getByLabelText("Start time"), "10:00");
  await user.type(screen.getByLabelText("End time"), "11:00");
  await user.type(screen.getByLabelText("Maximum capacity"), "18");
}

describe("CreateClassSession", () => {
  beforeEach(() => {
    vi.spyOn(tokenService, "getUser").mockReturnValue({ id: 12 });
    server.use(
      http.get("*/api/v1/gyms", () =>
        HttpResponse.json([{ id: 7, name: "Central Gym" }])
      )
    );
  });

  afterEach(() => {
    vi.restoreAllMocks();
  });

  test("submits a complete class schedule and returns to class management", async () => {
    const user = userEvent.setup();
    let requestBody;
    server.use(
      http.post("*/api/v1/class-sessions", async ({ request }) => {
        requestBody = await request.json();
        return HttpResponse.json({ id: 42 }, { status: 201 });
      })
    );

    renderPage();
    await fillRequiredFields(user);
    await user.click(screen.getByRole("button", { name: "Create class" }));

    expect(await screen.findByRole("heading", {
      name: "Class Management"
    })).toBeInTheDocument();
    expect(requestBody).toEqual({
      name: "Yoga",
      date: "2026-12-01",
      room: "CLASS_ROOM_1",
      startTime: "10:00",
      endTime: "11:00",
      maxCapacity: 18,
      gymId: 7
    });
  });

  test("shows a clear scheduling conflict without navigating away", async () => {
    const user = userEvent.setup();
    server.use(
      http.post("*/api/v1/class-sessions", () =>
        HttpResponse.json(
          { message: "The room is already occupied at this time" },
          { status: 409 }
        )
      )
    );

    renderPage();
    await fillRequiredFields(user);
    await user.click(screen.getByRole("button", { name: "Create class" }));

    expect(await screen.findByRole("alert")).toHaveTextContent(
      "Scheduling conflict: the selected room is already occupied during this time."
    );
    expect(screen.getByRole("heading", {
      name: "Create Class Schedule"
    })).toBeInTheDocument();
  });
});
