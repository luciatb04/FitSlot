import { render, screen } from "../../test-utils";
import userEvent from "@testing-library/user-event";
import { http, HttpResponse } from "msw";
import { server } from "../../mocks/server";
import Login from "./index";

describe("Login", () => {
  test("shows the authentication error returned by the backend", async () => {
    server.use(
      http.post("*/api/v1/auth/signin", () => HttpResponse.text("Bad Credentials!", { status: 400 }))
    );
    const user = userEvent.setup();
    render(<Login />);

    await user.type(await screen.findByLabelText("Username:"), "gym_owner1");
    await user.type(screen.getByLabelText("Password:"), "wrong-password");
    await user.click(screen.getByRole("button", { name: "Login" }));

    expect(await screen.findByRole("alert")).toHaveTextContent("Bad Credentials!");
  });
});
