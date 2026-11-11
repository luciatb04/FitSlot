import { render, screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { http, HttpResponse } from "msw";
import { server } from "../../mocks/server";
import MemberRegistration from "./MemberRegistration";

describe("MemberRegistration", () => {
  test("registers an active member and confirms success", async () => {
    let requestBody;
    server.use(
      http.post("*/api/v1/members", async ({ request }) => {
        requestBody = await request.json();
        return HttpResponse.json(
          { id: 12, ...requestBody, status: "ACTIVE" },
          { status: 201 }
        );
      })
    );
    const user = userEvent.setup();
    render(<MemberRegistration />);

    await user.type(screen.getByLabelText("First name"), " Ada ");
    await user.type(screen.getByLabelText("Last name"), " Lovelace ");
    await user.type(screen.getByLabelText("Email"), " ada@example.com ");
    await user.type(screen.getByLabelText("Telephone (optional)"), " 123456789 ");
    await user.click(screen.getByRole("button", { name: "Register member" }));

    expect(await screen.findByRole("status")).toHaveTextContent(
      "Ada Lovelace was registered with an active profile."
    );
    expect(requestBody).toEqual({
      firstName: "Ada",
      lastName: "Lovelace",
      email: "ada@example.com",
      telephone: "123456789"
    });
    expect(screen.getByLabelText("First name")).toHaveValue("");
  });

  test("requires first name, last name, and a valid email", () => {
    render(<MemberRegistration />);

    expect(screen.getByLabelText("First name")).toBeRequired();
    expect(screen.getByLabelText("Last name")).toBeRequired();
    expect(screen.getByLabelText("Email")).toBeRequired();
    expect(screen.getByLabelText("Email")).toHaveAttribute("type", "email");
    expect(screen.getByLabelText("Telephone (optional)")).not.toBeRequired();
  });

  test("shows a duplicate email error and preserves the form", async () => {
    server.use(
      http.post("*/api/v1/members", () => HttpResponse.json(
        { message: "Email already registered" },
        { status: 409 }
      ))
    );
    const user = userEvent.setup();
    render(<MemberRegistration />);

    await user.type(screen.getByLabelText("First name"), "Ada");
    await user.type(screen.getByLabelText("Last name"), "Lovelace");
    await user.type(screen.getByLabelText("Email"), "ada@example.com");
    await user.click(screen.getByRole("button", { name: "Register member" }));

    expect(await screen.findByRole("alert")).toHaveTextContent("Email already registered");
    expect(screen.getByLabelText("Email")).toHaveValue("ada@example.com");
  });

  test("explains that a forbidden response requires a receptionist account", async () => {
    server.use(
      http.post("*/api/v1/members", () => HttpResponse.json(
        { status: 403, error: "Forbidden" },
        { status: 403 }
      ))
    );
    const user = userEvent.setup();
    render(<MemberRegistration />);

    await user.type(screen.getByLabelText("First name"), "Ada");
    await user.type(screen.getByLabelText("Last name"), "Lovelace");
    await user.type(screen.getByLabelText("Email"), "ada@example.com");
    await user.click(screen.getByRole("button", { name: "Register member" }));

    expect(await screen.findByRole("alert")).toHaveTextContent(
      "Your account does not have permission to register members"
    );
    expect(screen.getByRole("alert")).toHaveTextContent("Sign out and sign in with a receptionist account");
  });
});
