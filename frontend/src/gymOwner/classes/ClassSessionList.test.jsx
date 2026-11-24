import { render, screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { http, HttpResponse } from "msw";
import { MemoryRouter } from "react-router-dom";
import { afterEach, vi } from "vitest";
import { server } from "../../mocks/server";
import ClassSessionList from "./ClassSessionList";

const session = {
  id: 42,
  name: "Yoga",
  date: "2026-12-01",
  startTime: "10:00:00",
  endTime: "11:00:00",
  room: "CLASS_ROOM_1",
  maxCapacity: 18,
  confirmedBookings: 4,
  instructorId: null,
  instructorName: null
};

function renderPage() {
  return render(
    <MemoryRouter>
      <ClassSessionList />
    </MemoryRouter>
  );
}

describe("ClassSessionList deletion", () => {
  afterEach(() => {
    vi.restoreAllMocks();
  });

  test("removes a class schedule after successful deletion", async () => {
    let deleted = false;
    vi.spyOn(window, "confirm").mockReturnValue(true);
    server.use(
      http.get("*/api/v1/class-sessions", () =>
        HttpResponse.json(deleted ? [] : [session])
      ),
      http.delete("*/api/v1/class-sessions/42", () => {
        deleted = true;
        return new HttpResponse(null, { status: 204 });
      })
    );

    const user = userEvent.setup();
    renderPage();
    expect(await screen.findByText("4 / 18")).toBeInTheDocument();
    await user.click(await screen.findByRole("button", {
      name: "Delete Yoga"
    }));

    expect(await screen.findByText("No scheduled classes found."))
      .toBeInTheDocument();
  });

  test("explains when a class with reservations cannot be deleted", async () => {
    vi.spyOn(window, "confirm").mockReturnValue(true);
    server.use(
      http.get("*/api/v1/class-sessions", () =>
        HttpResponse.json([session])
      ),
      http.delete("*/api/v1/class-sessions/42", () =>
        HttpResponse.json(
          { message: "Class sessions with reservations cannot be deleted" },
          { status: 409 }
        )
      )
    );

    const user = userEvent.setup();
    renderPage();
    await user.click(await screen.findByRole("button", {
      name: "Delete Yoga"
    }));

    expect(await screen.findByRole("alert")).toHaveTextContent(
      "This class has reservations and cannot be deleted."
    );
    expect(screen.getByText("Yoga")).toBeInTheDocument();
  });
});
