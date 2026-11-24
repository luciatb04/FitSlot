import { fireEvent, render, screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { http, HttpResponse } from "msw";
import { MemoryRouter, Route, Routes } from "react-router-dom";
import { server } from "../../mocks/server";
import EditClassSession from "./EditClassSession";

const session = {
  id: 42,
  name: "Yoga",
  date: "2026-12-01",
  dayOfWeek: "TUESDAY",
  startTime: "10:00:00",
  endTime: "11:00:00",
  room: "CLASS_ROOM_1",
  maxCapacity: 10,
  confirmedBookings: 4,
  availableSpots: 6,
  instructorId: null,
  instructorName: null
};

function renderPage() {
  return render(
    <MemoryRouter initialEntries={["/manage-classes/42/edit"]}>
      <Routes>
        <Route
          path="/manage-classes/:id/edit"
          element={<EditClassSession />}
        />
      </Routes>
    </MemoryRouter>
  );
}

describe("EditClassSession capacity", () => {
  beforeEach(() => {
    server.use(
      http.get("*/api/v1/class-sessions/42", () =>
        HttpResponse.json(session)
      )
    );
  });

  test("updates capacity and shows the confirmed booking count", async () => {
    let updateBody;
    server.use(
      http.put(
        "*/api/v1/class-sessions/42/capacity",
        async ({ request }) => {
          updateBody = await request.json();
          return HttpResponse.json({
            ...session,
            maxCapacity: 12,
            availableSpots: 8
          });
        }
      )
    );

    const user = userEvent.setup();
    renderPage();

    expect(await screen.findByText(/Currently booked: 4 of 10 places/))
      .toBeInTheDocument();
    fireEvent.change(screen.getByLabelText("New maximum capacity"), {
      target: { value: "12" }
    });
    await user.click(screen.getByRole("button", {
      name: "Update capacity"
    }));

    expect(await screen.findByRole("status")).toHaveTextContent(
      "Class capacity updated successfully."
    );
    expect(updateBody).toEqual({ maxCapacity: 12 });
    expect(screen.getByText(/Currently booked: 4 of 12 places/))
      .toBeInTheDocument();
  });

  test("rejects reducing capacity below bookings before sending a request", async () => {
    let requestSent = false;
    server.use(
      http.put("*/api/v1/class-sessions/42/capacity", () => {
        requestSent = true;
        return HttpResponse.json(session);
      })
    );

    const user = userEvent.setup();
    renderPage();
    await screen.findByText(/Currently booked: 4 of 10 places/);
    fireEvent.change(screen.getByLabelText("New maximum capacity"), {
      target: { value: "3" }
    });
    await user.click(screen.getByRole("button", {
      name: "Update capacity"
    }));

    expect(await screen.findByRole("alert")).toHaveTextContent(
      "Capacity cannot be lower than the 4 confirmed bookings."
    );
    expect(requestSent).toBe(false);
  });

  test("shows the backend conflict if booking counts changed after loading", async () => {
    server.use(
      http.put("*/api/v1/class-sessions/42/capacity", () =>
        HttpResponse.json(
          { message: "Capacity cannot be lower than confirmed bookings" },
          { status: 409 }
        )
      )
    );

    const user = userEvent.setup();
    renderPage();
    await screen.findByText(/Currently booked: 4 of 10 places/);
    fireEvent.change(screen.getByLabelText("New maximum capacity"), {
      target: { value: "5" }
    });
    await user.click(screen.getByRole("button", {
      name: "Update capacity"
    }));

    expect(await screen.findByRole("alert")).toHaveTextContent(
      "Capacity cannot be lower than the number of confirmed bookings."
    );
    expect(screen.getByText(/Currently booked: 4 of 10 places/))
      .toBeInTheDocument();
  });
});
