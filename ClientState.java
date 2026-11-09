import java.awt.*;
import java.awt.event.*;
import java.util.List;
import javax.swing.*;

/**
 * Screen and actions available to a warehouse client.
 * 
 * @author Aj Johnson, Aaditya Malla. Chidera Izuora
 * @version 1.0
 */
public class ClientState extends WarehouseState {
    private static ClientState instance;
    private final Warehouse warehouse = Warehouse.getInstance();

    /**
     * Private constructor to enforce singleton pattern.
     */
    private ClientState() {
        super();
    }

    /**
     * Returns the singleton instance of ClientState.
     * 
     * @return the singleton instance of ClientState
     */
    public static ClientState getInstance() {
        if (instance == null)
            instance = new ClientState();
        return instance;
    }

    /**
     * Displays the client interface, allowing the user to view details, products
     * and transactions, or go to wishlist operations (view/add/remove/modify
     * wishlist entries and place orders), which now live in {@link WishlistState}.
     */
    @Override
    public void run() {
        JFrame frame = prepareFrame();
        frame.setTitle("Warehouse - Client " + context.getUser());
        frame.getContentPane().setLayout(new BorderLayout()); // buttons on top, output below

        JPanel buttons = new JPanel(new FlowLayout());
        buttons.add(new DetailsButton());
        buttons.add(new ProductsButton());
        buttons.add(new TransactionsButton());
        buttons.add(new WishlistOpsButton());
        buttons.add(new LogoutButton());

        output.setEditable(false);
        frame.getContentPane().add(buttons, BorderLayout.NORTH);
        frame.getContentPane().add(new JScrollPane(output), BorderLayout.CENTER);
        showFrame(frame);
    }

    /**
     * Prepares the frame for the client state.
     *
     * @return the prepared JFrame
     */
    private void showClientDetails() {
        Client client = warehouse.getClient(context.getUser());
        if (client == null) {
            display("Client not found.");
            return;
        }
        display("Client ID: " + client.getID()
                + "\nName: " + client.getName()
                + "\nAddress: " + client.getAddress());
    }

    /**
     * Displays a list of all products available in the warehouse.
     */
    private void showProductList() {
        List<ProductView> views = warehouse.getProductListView(WarehouseContext.CLIENT);
        if (views.isEmpty()) {
            display("No products available.");
            return;
        }
        StringBuilder sb = new StringBuilder("Products:\n");
        for (ProductView pv : views)
            sb.append(pv.clientString()).append("\n");
        display(sb.toString());
    }

    /**
     * Displays a list of all transactions for the current client.
     */
    private void showClientTransactions() {
        Client client = warehouse.getClient(context.getUser());
        if (client == null) {
            display("Client not found.");
            return;
        }
        List<Transaction> ts = client.getTransactions();
        if (ts.isEmpty()) {
            display("No transactions found for this client.");
            return;
        }
        StringBuilder sb = new StringBuilder("Transactions:\n");
        for (Transaction t : ts)
            sb.append(t).append("\n");
        display(sb.toString());
    }

    /**
     * Moves to the wishlist-operations state. become() remembers this client
     * screen, so "Back to Client Menu" there returns here.
     */
    private void wishlistOperations() {
        context.become(WarehouseContext.WISHLIST, context.getUser());
    }

    /**
     * Button to display client details.
     */
    private class DetailsButton extends JButton implements ActionListener {
        DetailsButton() {
            super("Client details");
            addActionListener(this);
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            showClientDetails();
        }
    }

    /**
     * Button to display the product list.
     */
    private class ProductsButton extends JButton implements ActionListener {
        ProductsButton() {
            super("Show products");
            addActionListener(this);
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            showProductList();
        }
    }

    /**
     * Button to display client transactions.
     */
    private class TransactionsButton extends JButton implements ActionListener {
        TransactionsButton() {
            super("Transactions");
            addActionListener(this);
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            showClientTransactions();
        }
    }

    /**
     * Button to transition to the wishlist-operations state.
     */
    private class WishlistOpsButton extends JButton implements ActionListener {
        WishlistOpsButton() {
            super("Wishlist operations");
            addActionListener(this);
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            wishlistOperations();
        }
    }

    /**
     * Button to log out and return to the login state.
     */
    private class LogoutButton extends JButton implements ActionListener {
        LogoutButton() {
            super("Logout");
            addActionListener(this);
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            context.logout();
        }
    }
}
