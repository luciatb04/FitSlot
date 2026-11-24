import { render, screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { http, HttpResponse } from "msw";
import { server } from "../../mocks/server";
import CheckIn from "./CheckIn";

const member = {
  id: 12,
  firstName: "Ada",
  lastName: "Lovelace",
  email: "ada@example.com",
  status: "ACTIVE"
};

const activeSubscription = {
  id: 31,
  tierName: "Standard",
  status: "ACTIVE",
  verificationStatus: "ACTIVE",
  denialReason: null,
  startDate: "2026-11-01",
  endDate: "2026-12-01",
  startTime: "00:00:00",
  endTime: "23:59:00",
  timeSlotAllowed: true,
  timeSlotDenialReason: null,
  remainingEntries: null,
  allowedServices: ["WEIGHT_ROOM"]
};

const validMedicalCertificate = {
  expiryDate: "2026-12-31",
  validity: "VALID",
  denialReason: null
};

function mockMemberAndSubscription(
  subscription = activeSubscription,
  medicalCertificate = validMedicalCertificate
) {
  server.use(
    http.get("*/api/v1/members", () => HttpResponse.json([member])),
    http.get("*/api/v1/access/members/12/check-in-info", () => HttpResponse.json({
      checkedAt: "2026-11-24T11:00:00",
      medicalCertificate,
      subscriptions: [subscription]
    })),
    http.post("*/api/v1/access/validate", async ({ request }) => {
      const { serviceAccessed } = await request.json();
      const authorized =
        subscription.verificationStatus === "ACTIVE" &&
        subscription.timeSlotAllowed &&
        subscription.allowedServices.includes(serviceAccessed) &&
        medicalCertificate.validity === "VALID";
      return HttpResponse.json({
        authorized,
        reason: authorized ? null : "Subscription does not include this service"
      });
    })
  );
}

async function selectMember(user) {
  await user.type(
    screen.getByRole("textbox", { name: "Search by name, surname or member ID" }),
    "Ada"
  );
  await user.click(await screen.findByRole("button", { name: /Ada Lovelace/ }));
}

describe("CheckIn subscription verification", () => {
  test("shows the active status and expiry date, then displays successful check-in", async () => {
    mockMemberAndSubscription();
    let requestBody;
    server.use(
      http.post("*/api/v1/access/check-in", async ({ request }) => {
        requestBody = await request.json();
        return HttpResponse.json({
          accessRecordId: 42,
          memberId: 12,
          serviceAccessed: "WEIGHT_ROOM",
          result: "ALLOWED"
        });
      })
    );

    const user = userEvent.setup();
    render(<CheckIn />);
    await selectMember(user);

    expect(await screen.findAllByText("ACTIVE")).toHaveLength(2);
    expect(screen.getByText("01/12/2026")).toBeInTheDocument();
    expect(screen.getByText("ALLOWED")).toBeInTheDocument();
    expect(screen.getByLabelText("Schedule verification time")).toHaveTextContent(
      "24/11/2026 11:00"
    );
    expect(screen.getByLabelText("Medical certificate status: VALID")).toBeInTheDocument();
    expect(screen.getByText("31/12/2026")).toBeInTheDocument();
    await user.selectOptions(screen.getByLabelText("Service to access"), "WEIGHT_ROOM");
    expect(await screen.findByText("AUTHORIZED")).toBeInTheDocument();
    await user.click(screen.getByRole("button", { name: "Register check-in" }));

    expect(await screen.findByRole("alert")).toHaveTextContent(
      "Access allowed. Check-in #42."
    );
    expect(requestBody).toEqual({ memberId: 12, serviceAccessed: "WEIGHT_ROOM" });
  });

  test("shows an expired status and its denial reason without offering access", async () => {
    mockMemberAndSubscription({
      ...activeSubscription,
      verificationStatus: "EXPIRED",
      denialReason: "Subscription expired on 2026-11-20",
      timeSlotAllowed: true,
      timeSlotDenialReason: null,
      endDate: "2026-11-20"
    });
    const user = userEvent.setup();
    render(<CheckIn />);
    await selectMember(user);

    expect(await screen.findByText("EXPIRED")).toBeInTheDocument();
    expect(screen.getByLabelText("Subscription access denial reason")).toHaveTextContent(
      "Access denied: Subscription expired on 2026-11-20."
    );
    expect(screen.getByRole("button", { name: "Register check-in" })).toBeDisabled();
  });

  test("shows frozen subscriptions as denied", async () => {
    mockMemberAndSubscription({
      ...activeSubscription,
      status: "FROZEN",
      verificationStatus: "FROZEN",
      denialReason: "Subscription is frozen",
      timeSlotAllowed: true,
      timeSlotDenialReason: null
    });
    const user = userEvent.setup();
    render(<CheckIn />);
    await selectMember(user);

    expect(await screen.findByText("FROZEN")).toBeInTheDocument();
    expect(screen.getByLabelText("Subscription access denial reason")).toHaveTextContent(
      "Access denied: Subscription is frozen."
    );
    expect(screen.getByText("Current time-band status")).toBeInTheDocument();
    expect(screen.getByText("ALLOWED")).toBeInTheDocument();
    expect(screen.queryByLabelText("Schedule access denial reason")).not.toBeInTheDocument();
  });

  test("shows restricted schedule access and prevents check-in outside the permitted time band", async () => {
    mockMemberAndSubscription({
      ...activeSubscription,
      timeSlotAllowed: false,
      timeSlotDenialReason: "Access outside the allowed time band"
    });
    const user = userEvent.setup();
    render(<CheckIn />);
    await selectMember(user);

    expect(await screen.findByText("RESTRICTED")).toBeInTheDocument();
    expect(screen.getByLabelText("Schedule access denial reason")).toHaveTextContent(
      "Access restricted: Access outside the allowed time band."
    );
    expect(screen.getByRole("button", { name: "Register check-in" })).toBeDisabled();
  });

  test("shows an unauthorized result and reason for a service outside the subscription", async () => {
    mockMemberAndSubscription();
    let checkInCalled = false;
    server.use(
      http.post("*/api/v1/access/validate", () => HttpResponse.json({
        authorized: false,
        reason: "Subscription does not include this service"
      })),
      http.post("*/api/v1/access/check-in", () => {
        checkInCalled = true;
        return HttpResponse.json({ result: "ALLOWED" });
      })
    );
    const user = userEvent.setup();
    render(<CheckIn />);
    await selectMember(user);
    await user.selectOptions(screen.getByLabelText("Service to access"), "POOL");

    expect(await screen.findByText("UNAUTHORIZED")).toBeInTheDocument();
    expect(screen.getByRole("alert")).toHaveTextContent(
      "Access denied: Subscription does not include this service."
    );
    expect(screen.getByRole("button", { name: "Register check-in" })).toBeDisabled();
    expect(checkInCalled).toBe(false);
  });

  test("warns and prevents check-in when the medical certificate has expired", async () => {
    mockMemberAndSubscription(activeSubscription, {
      expiryDate: "2026-11-20",
      validity: "EXPIRED",
      denialReason: "Medical certificate has expired"
    });
    const user = userEvent.setup();
    render(<CheckIn />);
    await selectMember(user);

    expect(await screen.findByLabelText("Medical certificate status: EXPIRED")).toHaveClass(
      "checkin-pill-inactive"
    );
    expect(screen.getByLabelText("Medical certificate denial reason")).toHaveTextContent(
      "Access denied: Medical certificate has expired."
    );
    expect(screen.getByRole("button", { name: "Register check-in" })).toBeDisabled();
  });

  test("warns and prevents check-in when the member has no medical certificate", async () => {
    mockMemberAndSubscription(activeSubscription, {
      expiryDate: null,
      validity: "MISSING",
      denialReason: "No medical certificate registered"
    });
    const user = userEvent.setup();
    render(<CheckIn />);
    await selectMember(user);

    expect(await screen.findByLabelText("Medical certificate status: MISSING")).toHaveClass(
      "checkin-pill-inactive"
    );
    expect(screen.getByLabelText("Medical certificate denial reason")).toHaveTextContent(
      "Access denied: No medical certificate registered."
    );
    expect(screen.getByRole("button", { name: "Register check-in" })).toBeDisabled();
  });

  test("shows the backend reason when a check-in is rejected", async () => {
    mockMemberAndSubscription();
    server.use(
      http.post("*/api/v1/access/check-in", () => HttpResponse.json(
        { message: "Access denied: Subscription payment is not paid" },
        { status: 403 }
      ))
    );
    const user = userEvent.setup();
    render(<CheckIn />);
    await selectMember(user);
    await user.selectOptions(screen.getByLabelText("Service to access"), "WEIGHT_ROOM");
    expect(await screen.findByText("AUTHORIZED")).toBeInTheDocument();
    await user.click(screen.getByRole("button", { name: "Register check-in" }));

    expect(await screen.findByRole("alert")).toHaveTextContent(
      "Access denied: Subscription payment is not paid"
    );
  });
});
