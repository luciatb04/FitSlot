import { fireEvent, render, screen, within } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { http, HttpResponse } from "msw";
import { server } from "../../mocks/server";
import MedicalCertificateManagement from "./MedicalCertificateManagement";

const member = {
  id: 12,
  firstName: "Ada",
  lastName: "Lovelace",
  email: "ada@example.com",
  status: "ACTIVE"
};

describe("MedicalCertificateManagement", () => {
  test("registers a renewal and refreshes the member's certificate history", async () => {
    let certificates = [];
    server.use(
      http.get("*/api/v1/members", () => HttpResponse.json([member])),
      http.get("*/api/v1/members/12/medical-certificates", () => HttpResponse.json(certificates)),
      http.post("*/api/v1/members/12/medical-certificates", async ({ request }) => {
        const requestBody = await request.json();
        certificates = [{ id: 8, memberId: 12, ...requestBody, validity: "VALID" }];
        return HttpResponse.json(certificates[0], { status: 201 });
      })
    );
    const user = userEvent.setup();
    render(<MedicalCertificateManagement />);

    await user.selectOptions(await screen.findByLabelText("Select member"), "12");
    await user.type(await screen.findByLabelText("Issue date"), "2026-04-01");
    await user.type(screen.getByLabelText("Expiry date"), "2027-04-01");
    await user.click(screen.getByRole("button", { name: "Register renewal" }));

    expect(await screen.findByRole("status")).toHaveTextContent("Certificate renewal was registered.");
    expect(await screen.findByText("Current certificate")).toBeInTheDocument();
    expect(screen.getByText("Issued 2026-04-01 · Expires 2027-04-01")).toBeInTheDocument();
  });

  test("marks older certificates as historical and lets reception correct their dates", async () => {
    let updatedRequest;
    server.use(
      http.get("*/api/v1/members", () => HttpResponse.json([member])),
      http.get("*/api/v1/members/12/medical-certificates", () => HttpResponse.json([
        { id: 8, memberId: 12, issueDate: "2026-04-01", expiryDate: "2027-04-01", validity: "VALID" },
        { id: 7, memberId: 12, issueDate: "2025-01-01", expiryDate: "2025-12-31", validity: "EXPIRED" }
      ])),
      http.put("*/api/v1/members/12/medical-certificates/7", async ({ request }) => {
        updatedRequest = await request.json();
        return HttpResponse.json({
          id: 7,
          memberId: 12,
          ...updatedRequest,
          validity: "EXPIRED"
        });
      })
    );
    const user = userEvent.setup();
    render(<MedicalCertificateManagement />);

    await user.selectOptions(await screen.findByLabelText("Select member"), "12");
    const previousCertificate = await screen.findByText("Previous certificate");
    expect(within(previousCertificate.closest("li")).getByText(/does not determine access/)).toBeInTheDocument();
    await user.click(within(previousCertificate.closest("li")).getByRole("button", { name: "Correct dates" }));

    expect(screen.getByLabelText("Issue date")).toHaveValue("2025-01-01");
    fireEvent.change(screen.getByLabelText("Expiry date"), { target: { value: "2026-01-15" } });
    await user.click(screen.getByRole("button", { name: "Save correction" }));

    expect(await screen.findByRole("status")).toHaveTextContent("Certificate dates were corrected.");
    expect(updatedRequest).toEqual({ issueDate: "2025-01-01", expiryDate: "2026-01-15" });
  });
});
