import { render, screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { http, HttpResponse } from "msw";
import { server } from "../../mocks/server";
import PaymentManagement from "./PaymentManagement";

const member = {
  id: 21,
  firstName: "Ada",
  lastName: "Lovelace",
  email: "ada@example.com"
};

const durationTier = {
  id: 4,
  name: "Monthly",
  price: 35,
  durationDays: 30,
  entryQuota: null
};

function paymentRecord(overrides = {}) {
  return {
    id: 9,
    memberId: member.id,
    memberName: "Ada Lovelace",
    subscriptionId: 33,
    tierId: durationTier.id,
    tierName: durationTier.name,
    amount: 35,
    paymentDate: "2026-11-12T10:00:00",
    status: "PENDING",
    subscriptionStatus: "PENDING",
    startDate: "2026-11-12",
    endDate: "2026-12-11",
    remainingEntries: null,
    ...overrides
  };
}

function mockOptions() {
  server.use(
    http.get("*/api/v1/members", () => HttpResponse.json([member])),
    http.get("*/api/membership-tiers", () => HttpResponse.json([durationTier]))
  );
}

describe("PaymentManagement", () => {
  test("registers full-price payment with subscription and allows changing pending to paid", async () => {
    mockOptions();
    let payments = [];
    let createRequest;
    server.use(
      http.get("*/api/v1/payments", () => HttpResponse.json(payments)),
      http.post("*/api/v1/payments", async ({ request }) => {
        createRequest = await request.json();
        payments = [paymentRecord({
          status: createRequest.status,
          subscriptionStatus: createRequest.status === "PAID" ? "ACTIVE" : "PENDING",
          startDate: createRequest.startDate
        })];
        return HttpResponse.json(payments[0], { status: 201 });
      }),
      http.put("*/api/v1/payments/9/status", async ({ request }) => {
        const { status } = await request.json();
        payments = [paymentRecord({
          status,
          subscriptionStatus: status === "PAID" ? "ACTIVE" : "CANCELLED"
        })];
        return HttpResponse.json(payments[0]);
      })
    );
    const user = userEvent.setup();
    render(<PaymentManagement />);

    await user.selectOptions(await screen.findByLabelText("Member"), String(member.id));
    await user.selectOptions(screen.getByLabelText("Membership type"), String(durationTier.id));
    await user.selectOptions(screen.getByLabelText("Initial payment status"), "PENDING");
    await user.click(screen.getByRole("button", { name: "Register payment" }));

    expect(await screen.findByRole("status")).toHaveTextContent(
      "Payment and membership subscription were registered."
    );
    expect(createRequest).toMatchObject({
      memberId: member.id,
      tierId: durationTier.id,
      status: "PENDING",
      endDate: null
    });
    expect(screen.getAllByText("Monthly — 35.00 €")).toHaveLength(2);

    await user.click(screen.getByRole("button", { name: "Mark as paid" }));
    expect(await screen.findByRole("status")).toHaveTextContent("Payment marked as paid.");
    expect(screen.getByRole("button", { name: "Refund payment" })).toBeInTheDocument();
  });

  test("requires a manual end date for entry-based subscriptions", async () => {
    server.use(
      http.get("*/api/v1/members", () => HttpResponse.json([member])),
      http.get("*/api/membership-tiers", () => HttpResponse.json([
        { id: 5, name: "Ten visits", price: 50, durationDays: null, entryQuota: 10 }
      ])),
      http.get("*/api/v1/payments", () => HttpResponse.json([]))
    );
    const user = userEvent.setup();
    render(<PaymentManagement />);

    await user.selectOptions(await screen.findByLabelText("Member"), String(member.id));
    await user.selectOptions(screen.getByLabelText("Membership type"), "5");

    const endDate = screen.getByLabelText("Subscription end date");
    expect(endDate).toBeRequired();
    expect(screen.getByRole("button", { name: "Register payment" })).toBeDisabled();

    await user.type(endDate, "2026-12-31");
    expect(screen.getByRole("button", { name: "Register payment" })).toBeEnabled();
  });

  test("shows payment API failures without claiming success", async () => {
    mockOptions();
    server.use(
      http.get("*/api/v1/payments", () => HttpResponse.json([])),
      http.post("*/api/v1/payments", () => HttpResponse.json(
        { message: "An entry-based membership requires an end date on or after its start date" },
        { status: 400 }
      ))
    );
    const user = userEvent.setup();
    render(<PaymentManagement />);

    await user.selectOptions(await screen.findByLabelText("Member"), String(member.id));
    await user.selectOptions(screen.getByLabelText("Membership type"), String(durationTier.id));
    await user.click(screen.getByRole("button", { name: "Register payment" }));

    expect(await screen.findByRole("alert")).toHaveTextContent("requires an end date");
    expect(screen.queryByRole("status")).not.toBeInTheDocument();
  });
});
