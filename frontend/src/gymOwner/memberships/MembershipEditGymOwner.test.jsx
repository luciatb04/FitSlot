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

  test("saves selected services and access time through their separate API routes", async () => {
    const updates = [];
    server.use(
      http.get("*/api/membership-tiers", () => HttpResponse.json([initialTier])),
      http.get("*/api/membership-tiers/1/services", () => HttpResponse.json(["WEIGHT_ROOM"])),
      http.get("*/api/membership-tiers/1/time-band", () => HttpResponse.json("FULL_DAY")),
      http.put("*/api/membership-tiers/1/:setting", async ({ request, params }) => {
        updates.push({ setting: params.setting, body: await request.json() });
        return HttpResponse.json(params.setting === "services" ? ["POOL"] : "MORNINGS_ONLY");
      })
    );
    const user = userEvent.setup();
    render(<MembershipServices />);

    await user.click(await screen.findByRole("button", { name: "Edit services and time" }));
    const editSection = screen.getByRole("region", { name: "Edit services and access time" });
    await user.click(within(editSection).getByRole("checkbox", { name: "Swimming Pool" }));
    await user.click(within(editSection).getByRole("radio", { name: "Mornings only" }));
    await user.click(within(editSection).getByRole("button", { name: "Save changes" }));

    expect(await screen.findByRole("status")).toHaveTextContent("saved successfully");
    expect(updates).toEqual([
      { setting: "services", body: ["WEIGHT_ROOM", "POOL"] },
      { setting: "time-band", body: "MORNINGS_ONLY" }
    ]);
  });

  test("reports a partial save when updating the access time fails", async () => {
    server.use(
      http.get("*/api/membership-tiers", () => HttpResponse.json([initialTier])),
      http.get("*/api/membership-tiers/1/services", () => HttpResponse.json(["WEIGHT_ROOM"])),
      http.get("*/api/membership-tiers/1/time-band", () => HttpResponse.json("FULL_DAY")),
      http.put("*/api/membership-tiers/1/services", () => HttpResponse.json(["POOL"])),
      http.put("*/api/membership-tiers/1/time-band", () => HttpResponse.json(
        { message: "Time band update failed" },
        { status: 500 }
      ))
    );
    const user = userEvent.setup();
    render(<MembershipServices />);

    await user.click(await screen.findByRole("button", { name: "Edit services and time" }));
    const editSection = screen.getByRole("region", { name: "Edit services and access time" });
    await user.click(within(editSection).getByRole("checkbox", { name: "Swimming Pool" }));
    await user.click(within(editSection).getByRole("button", { name: "Save changes" }));

    expect(await screen.findByRole("alert")).toHaveTextContent(
      "Services were saved, but the access time was not"
    );
  });
});
