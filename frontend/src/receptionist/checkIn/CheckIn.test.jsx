import { render, screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { http, HttpResponse } from "msw";
import { server } from "../../mocks/server";
import CheckIn from "./CheckIn";

describe("CheckIn", () => {
  beforeEach(() => {
    server.use(
      http.get("*/api/v1/members", () => HttpResponse.json([
        {
          id: 12,
          firstName: "Ada",
          lastName: "Lovelace",
          email: "ada@example.com",
          status: "ACTIVE"
        }
      ]))
    );
  });

  test("shows allowed access when the check-in request succeeds", async () => {
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

    await user.selectOptions(await screen.findByLabelText("Member"), "12");
    await user.click(screen.getByRole("button", { name: "Check access" }));

    expect(await screen.findByRole("status")).toHaveTextContent("Access allowed. Record #42 was created.");
    expect(requestBody).toEqual({ memberId: 12, serviceAccessed: "WEIGHT_ROOM" });
  });

  test("shows the backend denial reason when the latest certificate is invalid", async () => {
    server.use(
      http.post("*/api/v1/access/check-in", () => HttpResponse.json(
        { message: "Access denied: latest medical certificate has expired" },
        { status: 403 }
      ))
    );
    const user = userEvent.setup();
    render(<CheckIn />);

    await user.selectOptions(await screen.findByLabelText("Member"), "12");
    await user.click(screen.getByRole("button", { name: "Check access" }));

    expect(await screen.findByRole("alert")).toHaveTextContent(
      "Access denied: latest medical certificate has expired"
    );
  });
});
