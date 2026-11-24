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

async function selectMember(user, query = "Ada") {
  await user.type(
    await screen.findByRole("searchbox", { name: "Search members" }),
    query
  );
  const memberRow = await screen.findByRole("row", {
    name: /Ada Lovelace.*ada@example\.com/
  });
  await user.click(within(memberRow).getByRole("button", { name: "Select Ada Lovelace" }));
}

describe("MedicalCertificateManagement", () => {
  test("shows the member directory below the search before filtering", async () => {
    server.use(
      http.get("*/api/v1/members", () => HttpResponse.json([
        member,
        {
          id: 13,
          firstName: "Grace",
          lastName: "Hopper",
          email: "grace@example.com",
          telephone: "555-0100",
          status: "ACTIVE"
        }
      ]))
    );

    render(<MedicalCertificateManagement />);

    expect(await screen.findByRole("heading", { name: "Members" }))
      .toBeInTheDocument();
    expect(screen.getByRole("searchbox", { name: "Search members" }))
      .toBeInTheDocument();
    expect(screen.getByRole("row", {
      name: /Ada Lovelace.*ada@example\.com/
    })).toBeInTheDocument();
    expect(screen.getByRole("row", {
      name: /Grace Hopper.*grace@example\.com/
    })).toBeInTheDocument();
  });

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

    await selectMember(user);
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

    await selectMember(user);
    const previousCertificate = await screen.findByText("Previous certificate");
    expect(within(previousCertificate.closest("li")).getByText(/does not determine access/)).toBeInTheDocument();
    await user.click(within(previousCertificate.closest("li")).getByRole("button", { name: "Correct dates" }));

    expect(screen.getByLabelText("Issue date")).toHaveValue("2025-01-01");
    fireEvent.change(screen.getByLabelText("Expiry date"), { target: { value: "2026-01-15" } });
    await user.click(screen.getByRole("button", { name: "Save correction" }));

    expect(await screen.findByRole("status")).toHaveTextContent("Certificate dates were corrected.");
    expect(updatedRequest).toEqual({ issueDate: "2025-01-01", expiryDate: "2026-01-15" });
  });

  test("finds members by email or ID and lets reception change the selection", async () => {
    server.use(
      http.get("*/api/v1/members", () => HttpResponse.json([member])),
      http.get("*/api/v1/members/12/medical-certificates", () => HttpResponse.json([]))
    );
    const user = userEvent.setup();
    render(<MedicalCertificateManagement />);

    await user.type(
      await screen.findByRole("searchbox", { name: "Search members" }),
      "12"
    );
    const result = await screen.findByRole("row", {
      name: /Ada Lovelace.*ada@example\.com.*12/
    });
    await user.click(within(result).getByRole("button", { name: "Select Ada Lovelace" }));

    expect(await screen.findByRole("heading", {
      name: "Certificate history"
    })).toBeInTheDocument();
    await user.click(screen.getByRole("button", { name: "Change member" }));
    expect(screen.getByRole("searchbox", { name: "Search members" }))
      .toHaveValue("");
  });
});
