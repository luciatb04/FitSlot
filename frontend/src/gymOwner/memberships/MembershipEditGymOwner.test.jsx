import { render, screen, waitFor, within } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { http, HttpResponse } from "msw";
import { server } from "../../mocks/server";
import MembershipServices from "./MembershipEditGymOwner";

const initialTier = {
  id: 1,
  name: "Standard",
  price: 30,
  durationDays: 30,
  entryQuota: null,
  allowedServices: ["WEIGHT_ROOM"],
  timeBand: "FULL_DAY"
};

beforeEach(() => {
  server.use(
    http.get("*/api/time-bands", () => HttpResponse.json([
      { timeBand: "FULL_DAY", startTime: "00:00:00", endTime: "23:59:00" },
      { timeBand: "MORNINGS_ONLY", startTime: "06:00:00", endTime: "12:00:00" },
      { timeBand: "AFTERNOONS_ONLY", startTime: "12:00:00", endTime: "18:00:00" },
      { timeBand: "EVENINGS_ONLY", startTime: "18:00:00", endTime: "23:59:00" }
    ]))
  );
});

function mockMembershipApi(tiers = [initialTier]) {
  server.use(
    http.get("*/api/membership-tiers", () => HttpResponse.json(tiers)),
    http.get("*/api/membership-tiers/:id/services", () => HttpResponse.json(["WEIGHT_ROOM"])),
    http.get("*/api/membership-tiers/:id/time-band", () => HttpResponse.json("FULL_DAY"))
  );
}

describe("MembershipServices", () => {
  test("displays membership cards and opens the create form", async () => {
    mockMembershipApi();
    const user = userEvent.setup();
    render(<MembershipServices />);

    expect(await screen.findByRole("heading", { name: "Standard" })).toBeInTheDocument();
    await user.click(screen.getByRole("button", { name: "Create membership" }));

    expect(screen.getByRole("heading", { name: "Create a membership type" })).toBeInTheDocument();
    expect(screen.getByLabelText("Duration (days)")).toBeInTheDocument();
  });

  test("creates a duration-based membership with its selected services", async () => {
    let requestBody;
    server.use(
      http.get("*/api/membership-tiers", () => HttpResponse.json([initialTier])),
      http.post("*/api/membership-tiers", async ({ request }) => {
        requestBody = await request.json();
        return HttpResponse.json({ id: 2, ...requestBody }, { status: 201 });
      })
    );
    const user = userEvent.setup();
    render(<MembershipServices />);

    await user.click(await screen.findByRole("button", { name: "Create membership" }));
    await user.type(screen.getByLabelText("Name"), "Pool monthly");
    await user.type(screen.getByLabelText("Price"), "45");
    await user.type(screen.getByLabelText("Duration (days)"), "30");
    await user.click(screen.getByRole("checkbox", { name: "Swimming Pool" }));
    await user.click(screen.getByRole("button", { name: "Save membership" }));

    expect(await screen.findByRole("status")).toHaveTextContent("created successfully");
    expect(requestBody).toMatchObject({
      name: "Pool monthly",
      price: 45,
      durationDays: 30,
      entryQuota: null,
      allowedServices: ["POOL"],
      timeBand: "FULL_DAY"
    });
    expect(screen.getByRole("heading", { name: "Pool monthly" })).toBeInTheDocument();
  });

  test("creates an entry-based membership and omits duration", async () => {
    let requestBody;
    server.use(
      http.get("*/api/membership-tiers", () => HttpResponse.json([initialTier])),
      http.post("*/api/membership-tiers", async ({ request }) => {
        requestBody = await request.json();
        return HttpResponse.json({ id: 3, ...requestBody }, { status: 201 });
      })
    );
    const user = userEvent.setup();
    render(<MembershipServices />);

    await user.click(await screen.findByRole("button", { name: "Create membership" }));
    await user.type(screen.getByLabelText("Name"), "Ten visits");
    await user.type(screen.getByLabelText("Price"), "60");
    await user.click(screen.getByRole("radio", { name: "By entries" }));
    await user.type(screen.getByLabelText("Number of entries"), "10");
    await user.click(screen.getByRole("checkbox", { name: "Weight Room" }));
    await user.click(screen.getByRole("button", { name: "Save membership" }));

    await waitFor(() => expect(requestBody).toBeDefined());
    expect(requestBody.durationDays).toBeNull();
    expect(requestBody.entryQuota).toBe(10);
  });

  test("requires a service before creating a membership", async () => {
    mockMembershipApi();
    const user = userEvent.setup();
    render(<MembershipServices />);

    await user.click(await screen.findByRole("button", { name: "Create membership" }));
    expect(screen.getByRole("button", { name: "Save membership" })).toBeDisabled();
  });

  test("shows the backend error and keeps the entered form values", async () => {
    server.use(
      http.get("*/api/membership-tiers", () => HttpResponse.json([])),
      http.post("*/api/membership-tiers", () => HttpResponse.json(
        { message: "A membership with this name already exists" },
        { status: 400 }
      ))
    );
    const user = userEvent.setup();
    render(<MembershipServices />);

    await user.click(await screen.findByRole("button", { name: "Create membership" }));
    await user.type(screen.getByLabelText("Name"), "Existing plan");
    await user.type(screen.getByLabelText("Price"), "10");
    await user.type(screen.getByLabelText("Duration (days)"), "7");
    await user.click(screen.getByRole("checkbox", { name: "Weight Room" }));
    await user.click(screen.getByRole("button", { name: "Save membership" }));

    expect(await screen.findByRole("alert")).toHaveTextContent("already exists");
    expect(screen.getByLabelText("Name")).toHaveValue("Existing plan");
  });

  test("saves selected services and access time through the configuration API", async () => {
    let updateRequest;
    server.use(
      http.get("*/api/membership-tiers", () => HttpResponse.json([initialTier])),
      http.put("*/api/membership-tiers/1/configuration", async ({ request }) => {
        updateRequest = await request.json();
        return HttpResponse.json({
          ...initialTier,
          ...updateRequest
        });
      })
    );
    const user = userEvent.setup();
    render(<MembershipServices />);

    await screen.findByRole("heading", { name: "Standard" });
    await user.click(screen.getByRole("button", { name: "Edit membership" }));
    const editSection = screen.getByRole("region", { name: "Edit services and access time" });
    await user.click(within(editSection).getByRole("checkbox", { name: "Swimming Pool" }));
    await user.click(within(editSection).getByRole("radio", { name: "Mornings only" }));
    await user.clear(within(editSection).getByLabelText("Start time"));
    await user.type(within(editSection).getByLabelText("Start time"), "07:00");
    await user.clear(within(editSection).getByLabelText("End time"));
    await user.type(within(editSection).getByLabelText("End time"), "11:30");
    await user.click(within(editSection).getByRole("button", { name: "Save changes" }));

    expect(await screen.findByRole("status")).toHaveTextContent("saved successfully");
    expect(updateRequest).toEqual({
      allowedServices: ["WEIGHT_ROOM", "POOL"],
      timeBand: "MORNINGS_ONLY",
      startTime: "07:00",
      endTime: "11:30"
    });
  });

  test("reports an error when saving membership configuration fails", async () => {
    server.use(
      http.get("*/api/membership-tiers", () => HttpResponse.json([initialTier])),
      http.put("*/api/membership-tiers/1/configuration", () => HttpResponse.json(
        { message: "Membership configuration update failed" },
        { status: 500 }
      ))
    );
    const user = userEvent.setup();
    render(<MembershipServices />);

    await screen.findByRole("heading", { name: "Standard" });
    await user.click(screen.getByRole("button", { name: "Edit membership" }));
    const editSection = screen.getByRole("region", { name: "Edit services and access time" });
    await user.click(within(editSection).getByRole("checkbox", { name: "Swimming Pool" }));
    await user.click(within(editSection).getByRole("button", { name: "Save changes" }));

    expect(await screen.findByRole("alert")).toHaveTextContent("Membership configuration update failed");
  });
});
