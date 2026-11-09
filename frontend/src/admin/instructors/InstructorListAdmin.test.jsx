import { render, screen, testRenderList } from "../../test-utils";
import userEvent from "@testing-library/user-event";
import InstructorListAdmin from "./InstructorListAdmin";

describe('InstructorListAdmin', () => {
    test('renders correctly', async () => {
        render(<InstructorListAdmin />);
        testRenderList('instructors');
    });

    test('renders instructors correctly', async () => {
        render(<InstructorListAdmin />);
        const instructor1 = await screen.findByRole('cell', { 'name': 'instructor1' });
        expect(instructor1).toBeInTheDocument();

        const editButtons = await screen.findAllByRole('link', { 'name': /edit/ });
        expect(editButtons).toHaveLength(2);

        const deleteButtons = await screen.findAllByRole('button', { 'name': /delete/ });
        expect(deleteButtons).toHaveLength(2)

        const instructor2 = await screen.findByRole('cell', { 'name': 'instructor2' });
        expect(instructor2).toBeInTheDocument();

        const instructors = await screen.findAllByRole('row', {},);
        expect(instructors).toHaveLength(3);
    });

    test('delete instructor correct', async () => {
        const user = userEvent.setup();
        const jsdomConfirm = window.confirm;
        window.confirm = () => { return true };
        render(<InstructorListAdmin />);

        const instructor1Delete = await screen.findByRole('button', { 'name': 'delete-1' });
        await user.click(instructor1Delete);
        const alert = await screen.findByRole('alert');
        expect(alert).toBeInTheDocument();

        window.confirm = jsdomConfirm;
    });
});