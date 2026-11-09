import java.awt.*;
import java.awt.event.*;
import java.util.ArrayList;
import java.util.List;
import javax.swing.*;

/**
 * Screen for wishlist operations, reached from the client menu with
 * "Wishlist operations". From here the current client can view, add, remove
 * and modify wishlist entries, and place an order from the wishlist.
 * "Back to Client Menu" returns to {@link ClientState}.
 *
 * <p>
 * The client being served is the one stored in the {@link WarehouseContext}
 * ({@code context.getUser()}), so this state works the same whether the client
 * logged in directly or a clerk used "Become Client".
 * </p>
 *
 * @author Aj Johnson, Aaditya Malla, Chidera Izuora
 * @version 1.0
 */
public class WishlistState extends WarehouseState {
    private static WishlistState instance;
    private final Warehouse warehouse = Warehouse.getInstance();

    /**
     * Private constructor to enforce singleton pattern.
     */
    private WishlistState() {
        super();
    }

    /**
     * Returns the singleton instance of WishlistState.
     * 
     * @return the singleton instance of WishlistState
     */
    public static WishlistState getInstance() {
        if (instance == null)
            instance = new WishlistState();
        return instance;
    }

    /**
     * Builds the wishlist-operations screen on the shared window.
     */
    @Override
    public void run() {
        JFrame frame = prepareFrame();
        frame.setTitle("Warehouse - Wishlist operations for client " + context.getUser());
        frame.getContentPane().setLayout(new BorderLayout()); // buttons on top, output below

        JPanel buttons = new JPanel(new FlowLayout());
        buttons.add(new ShowWishlistButton());
        buttons.add(new AddItemButton());
        buttons.add(new RemoveItemButton());
        buttons.add(new ModifyQuantityButton());
        buttons.add(new PlaceOrderButton());
        buttons.add(new BackButton());

        output.setEditable(false);
        frame.getContentPane().add(buttons, BorderLayout.NORTH);
        frame.getContentPane().add(new JScrollPane(output), BorderLayout.CENTER);
        showFrame(frame);
        showWishlist(); // show the current wishlist as soon as the screen opens
    }

    /**
     * Displays the current client's wishlist with product name, price,
     * requested quantity and current stock.
     */
    private void showWishlist() {
        List<WishlistView> views = warehouse.getClientWishlistView(context.getUser());
        if (views == null) {
            display("Client not found.");
            return;
        }
        if (views.isEmpty()) {
            display("Wishlist for " + context.getUser() + " is empty.");
            return;
        }
        StringBuilder sb = new StringBuilder("Wishlist for " + context.getUser() + ":\n");
        for (WishlistView v : views)
            sb.append(String.format("Product=%s, Name=%s, Price=$%.2f, Qty=%d%n",
                    v.getProductID(), v.getProductName(), v.getPrice(), v.getWishlistQuantity()));
        display(sb.toString());
    }

    /**
     * Adds a quantity of a product to the current client's wishlist.
     */
    private void addItem() {
        String productID = promptRequired("Enter product ID:");
        if (productID == null) {
            display("Add cancelled.");
            return;
        }
        Integer qty = promptInt("Enter quantity:", 1);
        if (qty == null) {
            display("Add cancelled.");
            return;
        }

        if (warehouse.addToWishlist(context.getUser(), productID, qty)) {
            showWishlist();
            output.insert("Added " + qty + " x " + productID + ".\n\n", 0);
        } else
            display("Error: product " + productID + " not found.");
    }

    /**
     * Removes a product from the current client's wishlist.
     */
    private void removeItem() {
        String productID = promptRequired("Enter product ID to remove:");
        if (productID == null) {
            display("Remove cancelled.");
            return;
        }

        if (warehouse.removeFromWishlist(context.getUser(), productID)) {
            showWishlist();
            output.insert("Removed " + productID + ".\n\n", 0);
        } else
            display("Product " + productID + " is not on the wishlist.");
    }

    /**
     * Changes the quantity of a product already on the wishlist.
     * A quantity of 0 removes the product.
     */
    private void modifyQuantity() {
        String productID = promptRequired("Enter product ID to modify:");
        if (productID == null) {
            display("Modify cancelled.");
            return;
        }
        Integer qty = promptInt("Enter new quantity (0 removes the item):", 0);
        if (qty == null) {
            display("Modify cancelled.");
            return;
        }

        if (warehouse.updateWishlistQuantity(context.getUser(), productID, qty)) {
            showWishlist();
            output.insert("Updated " + productID + ".\n\n", 0);
        } else
            display("Product " + productID + " is not on the wishlist.");
    }

    /**
     * Places an order from the wishlist. The client can order the whole
     * wishlist at the listed quantities in one step, or go item by item and
     * choose a quantity for each.
     */
    private void placeOrder() {
        String clientID = context.getUser();
        List<WishlistItem> wishlist = warehouse.getClientWishlist(clientID);
        if (wishlist == null) {
            display("Client not found.");
            return;
        }
        if (wishlist.isEmpty()) {
            display("Wishlist is empty.");
            return;
        }

        int all = JOptionPane.showConfirmDialog(context.getFrame(),
                "Order everything on the wishlist at the listed quantities?\n"
                        + "(No = choose item by item)",
                "Place order", JOptionPane.YES_NO_CANCEL_OPTION);
        if (all != JOptionPane.YES_OPTION && all != JOptionPane.NO_OPTION) {
            display("Order cancelled.");
            return;
        }

        List<OrderLine> lines = new ArrayList<>();
        for (WishlistItem w : wishlist) // createOrder changes the wishlist only after we finish
        {
            if (all == JOptionPane.YES_OPTION) {
                lines.add(new OrderLine(w.getProductID(), w.getQuantity()));
                continue;
            }
            int choice = JOptionPane.showConfirmDialog(context.getFrame(),
                    "Order " + w.getProductID() + " (wishlist qty " + w.getQuantity() + ")?",
                    "Place order", JOptionPane.YES_NO_CANCEL_OPTION);
            if (choice == JOptionPane.NO_OPTION)
                continue;
            if (choice != JOptionPane.YES_OPTION) {
                display("Order cancelled.");
                return;
            }

            Integer qty = promptInt("Quantity of " + w.getProductID()
                    + " to order (wishlist has " + w.getQuantity() + "):", 1);
            if (qty == null) {
                display("Order cancelled.");
                return;
            }
            lines.add(new OrderLine(w.getProductID(), qty));
        }

        if (lines.isEmpty()) {
            display("Nothing was selected; no order placed.");
            return;
        }
        display("Order for " + clientID + ":\n" + warehouse.createOrder(clientID, lines));
    }

    /**
     * Returns to the client menu.
     */
    private void back() {
        context.logout(); // pops the client entry pushed when we came here
    }

    /** Button to display the wishlist. */
    private class ShowWishlistButton extends JButton implements ActionListener {
        ShowWishlistButton() {
            super("Show wishlist");
            addActionListener(this);
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            showWishlist();
        }
    }

    /** Button to add an item to the wishlist. */
    private class AddItemButton extends JButton implements ActionListener {
        AddItemButton() {
            super("Add item");
            addActionListener(this);
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            addItem();
        }
    }

    /** Button to remove an item from the wishlist. */
    private class RemoveItemButton extends JButton implements ActionListener {
        RemoveItemButton() {
            super("Remove item");
            addActionListener(this);
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            removeItem();
        }
    }

    /** Button to change the quantity of a wishlist item. */
    private class ModifyQuantityButton extends JButton implements ActionListener {
        ModifyQuantityButton() {
            super("Modify quantity");
            addActionListener(this);
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            modifyQuantity();
        }
    }

    /** Button to place an order from the wishlist. */
    private class PlaceOrderButton extends JButton implements ActionListener {
        PlaceOrderButton() {
            super("Place order");
            addActionListener(this);
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            placeOrder();
        }
    }

    /** Button to return to the client menu. */
    private class BackButton extends JButton implements ActionListener {
        BackButton() {
            super("Back to Client Menu");
            addActionListener(this);
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            back();
        }
    }
}
