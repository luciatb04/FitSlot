import { render, screen, testRenderList } from "../../test-utils";
import userEvent from "@testing-library/user-event";
import ClassListAdmin from "./ClassListAdmin";

describe('ClassListAdmin', () => {
    test('renders correctly', async () => {
        render(<ClassListAdmin />);
        testRenderList('classes');
    });

    test('renders classes correctly', async () => {
        render(<ClassListAdmin />);
        const class1 = await screen.findByRole('cell', { 'name': 'Leo' });
        expect(class1).toBeInTheDocument();

        const editButtons = await screen.findAllByRole('link', { 'name': /edit/ });
        expect(editButtons).toHaveLength(2);

        const deleteButtons = await screen.findAllByRole('button', { 'name': /delete/ });
        expect(deleteButtons).toHaveLength(2);

        const class2 = await screen.findByRole('cell', { 'name': 'Basil' });
        expect(class2).toBeInTheDocument();

        const classes = await screen.findAllByRole('row', {},);
        expect(classes).toHaveLength(3);
    });

    test('delete class correct', async () => {
        const user = userEvent.setup();
        const jsdomConfirm = window.confirm;
        window.confirm = () => { return true };
        render(<ClassListAdmin />);

        const class1Delete = await screen.findByRole('button', { 'name': 'delete-1' });
        await user.click(class1Delete);
        const alert = await screen.findByRole('alert');
        expect(alert).toBeInTheDocument();

        window.confirm = jsdomConfirm;
    });
});