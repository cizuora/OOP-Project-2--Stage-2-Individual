import java.util.ArrayList;
import java.util.List;

/**
 * The facade and single entry point of the warehouse system.
 *
 * <p>
 * The {@link UserInterface} (and the test drivers) talk only to this class;
 * it in turn delegates to the {@link ClientList} and the {@link Catalog}, which
 * own the client and product collections. This keeps the user interface from
 * depending on how clients, products, wishlists, waitlists, and account
 * histories are stored.
 * </p>
 *
 * <p>
 * Stage 1 operations: add a client, add a product, add a product to a
 * client's wishlist, and the queries that print clients, products, and a
 * wishlist. Stage 2 operations: process a client's order
 * ({@link #createOrder(String, List)}), receive a shipment
 * ({@link #receiveShipment(List)}), and receive a payment
 * ({@link #receivePayment(String, double)}).
 * </p>
 *
 * <p>
 * Money is recorded as signed {@link Transaction}s on the client's account:
 * charges are negative and payments are positive, so a negative balance means
 * the client owes money and a positive balance is a credit.
 * </p>
 */
public class Warehouse {
    /** All registered clients. */
    private final ClientList clientList = new ClientList();

    /** All products the warehouse carries. */
    private final Catalog catalog = new Catalog();

    private static final Warehouse instance = new Warehouse();

    private Warehouse() {
    } // Private constructor to enforce singleton pattern

    /**
     * Returns the singleton warehouse instance.
     *
     * @return the shared warehouse
     */
    public static Warehouse getInstance() {
        return instance;
    }

    /**
     * Registers a new client.
     *
     * @param name    the client's name
     * @param address the client's address
     * @return the id generated for the new client
     */
    public String addClient(String name, String address) {
        Client c = new Client(name, address); // Client generates its own ID
        clientList.addClient(c);
        return c.getID();
    }

    /**
     * Adds a new product to the catalog.
     *
     * @param name  the product's display name
     * @param price the unit sale price in dollars
     * @param stock the number of units initially on hand
     * @return the id generated for the new product
     */
    public String addProduct(String name, double price, int stock) {
        Product p = new Product(name, price, stock);
        catalog.addProduct(p);
        return p.getID();
    }

    /**
     * Adds one unit of a product to a client's wishlist.
     *
     * @param clientID  the id of the client
     * @param productID the id of the product
     * @return {@code true} if the entry was added; {@code false} if either id is
     *         unknown
     * @see #addToWishlist(String, String, int)
     */
    public boolean addToWishlist(String clientID, String productID) {
        return addToWishlist(clientID, productID, 1);
    }

    /**
     * Adds a quantity of a product to a client's wishlist, after checking that
     * both the client and the product exist. If the product is already on the
     * wishlist, the quantity is added to the existing entry.
     *
     * @param clientID  the id of the client
     * @param productID the id of the product
     * @param quantity  the number of units to add
     * @return {@code true} if the entry was added; {@code false} if either id is
     *         unknown
     */
    public boolean addToWishlist(String clientID, String productID, int quantity) {
        if (clientList.searchForClient(clientID) == null)
            return false; // validate client
        if (catalog.searchForProduct(productID) == null)
            return false; // validate product
        return clientList.addClientWishlistEntry(clientID, productID, quantity);
    }

    /**
     * Removes a product from a client's wishlist.
     *
     * @param clientID  the id of the client
     * @param productID the id of the product to remove
     * @return {@code true} if the entry was removed; {@code false} if the client
     *         is unknown or the product is not on the wishlist
     */
    public boolean removeFromWishlist(String clientID, String productID) {
        return clientList.removeClientWishlistEntry(clientID, productID);
    }

    /**
     * Changes the quantity of a product on a client's wishlist. A quantity of
     * zero or less removes the product from the wishlist.
     *
     * @param clientID  the id of the client
     * @param productID the id of the product
     * @param quantity  the new quantity
     * @return {@code true} if the wishlist was changed; {@code false} if the
     *         client is unknown or the product is not on the wishlist
     */
    public boolean updateWishlistQuantity(String clientID, String productID, int quantity) {
        if (quantity <= 0)
            return removeFromWishlist(clientID, productID);
        return clientList.setClientWishlistQuantity(clientID, productID, quantity);
    }

    /**
     * Returns a list of all clients.
     * 
     * @return a list of all clients
     */
    public List<Client> getAllClients() {
        return clientList.getAllClients();
    }

    /**
     * Returns a list of all products.
     * 
     * @return a list of all products
     */
    public List<Product> getAllProducts() {
        return catalog.getAllProducts();
    }

    /**
     * Returns a list of wishlist views for a client.
     * 
     * @param clientID the id of the client
     * @return a list of wishlist views for the client
     */
    public List<WishlistView> getClientWishlistView(String clientID) {
        List<WishlistItem> items = clientList.getClientWishlist(clientID);
        if (items == null)
            return null;
        List<WishlistView> views = new ArrayList<>();
        for (WishlistItem item : items) {
            Product p = catalog.searchForProduct(item.getProductID());
            if (p == null)
                continue;
            views.add(
                    new WishlistView(item.getProductID(), p.getName(), p.getPrice(), item.getQuantity(), p.getStock()));
        }
        return views;
    }

    /**
     * Returns a list of waitlist views for a product.
     * 
     * @param productID the id of the product
     * @return a list of waitlist views for the product
     */
    public List<WaitlistView> getProductWaitlistView(String productID) {
        Product p = catalog.searchForProduct(productID);
        if (p == null)
            return null;
        List<WaitlistItem> items = p.getWaitlist();
        if (items == null)
            return null;
        List<WaitlistView> views = new ArrayList<>();
        for (WaitlistItem item : items) {
            Client c = clientList.searchForClient(item.getClientID());
            if (c == null)
                continue;
            views.add(new WaitlistView(item.getClientID(), productID, item.getQuantity(),
                    p.getName(), p.getPrice(), p.getStock()));
        }
        return views;
    }

    /**
     * Returns the waitlist for a product.
     * 
     * @param productID the id of the product
     * @return the waitlist for the product
     */
    public List<WaitlistItem> getProductWaitlist(String productID) {
        Product p = catalog.searchForProduct(productID);
        return (p == null) ? null : p.getWaitlist();
    }

    /**
     * Records a transaction on a client's account.
     *
     * @param clientID    the id of the client
     * @param type        the category of activity (e.g. {@code "PAYMENT"})
     * @param description a human-readable detail
     * @param amount      the signed amount: positive to credit the client, negative
     *                    to charge
     * @return {@code true} if the transaction was recorded; {@code false} if the
     *         client is unknown
     */
    public boolean recordTransaction(String clientID, String type, String description, double amount) {
        Client c = clientList.searchForClient(clientID);
        if (c == null)
            return false;
        c.addTransaction(new Transaction(type, description, amount));
        return true;
    }

    /**
     * Returns a client's transaction history, oldest first.
     *
     * @param clientID the id of the client
     * @return a copy of the client's transactions, or {@code null} if the client is
     *         unknown
     */
    public List<Transaction> getClientTransactions(String clientID) {
        Client c = clientList.searchForClient(clientID);
        return (c == null) ? null : c.getTransactions();
    }

    /**
     * Prints the transaction history for a client.
     * 
     * @param clientID the id of the client
     */
    public void printClientTransactions(String clientID) {
        List<Transaction> ts = getClientTransactions(clientID);
        if (ts == null) {
            System.out.println("Client not found.");
            return;
        }
        if (ts.isEmpty()) {
            System.out.println("No transactions.");
            return;
        }
        for (Transaction t : ts)
            System.out.println(t);
    }

    /**
     * Returns a client's wishlist. Used by the user interface to walk the clerk
     * through the wishlist when processing an order.
     *
     * <p>
     * This is the client's actual wishlist, not a copy (see
     * {@link ClientList#getClientWishlist(String)}).
     * </p>
     *
     * @param clientID the id of the client
     * @return the client's wishlist entries, or {@code null} if the client is
     *         unknown
     */
    public List<WishlistItem> getClientWishlist(String clientID) {
        Client c = clientList.searchForClient(clientID);
        if (c == null)
            return null;
        return clientList.getClientWishlist(clientID);
    }

    /**
     * Processes a client's order (Stage 2, "process a client's order").
     *
     * <p>
     * For each order line:
     * </p>
     * <ul>
     * <li>A quantity of zero or less is treated as "not ordered", and the
     * product is removed from the wishlist.</li>
     * <li>Otherwise as many units as are in stock are filled (stock is reduced
     * and the cost is added to the order total), and any shortfall is
     * placed on the product's waitlist for this client.</li>
     * <li>If the ordered quantity covers the whole wishlist quantity, the
     * product is removed from the wishlist; otherwise the wishlist quantity
     * is reduced by the amount ordered.</li>
     * </ul>
     * <p>
     * Wishlist changes are applied after all lines are processed. If any units
     * were filled, a single {@code "ORDER"} transaction charges the client the
     * total cost (recorded as a negative amount). Lines for unknown products are
     * skipped.
     * </p>
     *
     * @param clientID the id of the ordering client
     * @param lines    the products and quantities being ordered
     * @return a report listing what was filled and waitlisted and the total
     *         charged, or {@code "Client not found."}
     */
    public String createOrder(String clientID, List<OrderLine> lines) {
        Client c = clientList.searchForClient(clientID);
        if (c == null)
            return "Client not found.";

        double total = 0.0;
        StringBuilder report = new StringBuilder();
        List<String> toRemove = new ArrayList<>(); // productIDs to drop from the wishlist
        List<OrderLine> toDecrement = new ArrayList<>(); // items to reduce, not remove

        for (OrderLine line : lines) {
            String pid = line.getProductID();
            int ordered = line.getQuantity();

            // Find the wishlist quantity for this item so we know remove vs decrement.
            int wishQty = c.getWishlistQuantity(pid); // small helper on Client, 0 if absent

            if (ordered <= 0) {
                toRemove.add(pid); // not ordered -> remove
                continue;
            }

            // Fill against stock, waitlist the shortfall, debit.
            Product p = catalog.searchForProduct(pid);
            if (p == null)
                continue;
            int filled = Math.min(ordered, p.getStock());
            int shortfall = ordered - filled;
            if (filled > 0) {
                p.setStock(p.getStock() - filled);
                total += filled * p.getPrice();
                report.append("Filled ").append(filled).append(" x ").append(p.getName()).append("\n");
            }
            if (shortfall > 0) {
                p.addToWaitlist(clientID, shortfall);
                report.append("Waitlisted ").append(shortfall).append(" x ").append(p.getName()).append("\n");
            }

            // Wishlist bookkeeping: full order clears it, partial reduces it.
            if (ordered >= wishQty)
                toRemove.add(pid);
            else
                toDecrement.add(new OrderLine(pid, wishQty - ordered));
        }

        // Apply all wishlist changes after the loop
        for (String pid : toRemove)
            c.removeWishlistEntry(pid);
        for (OrderLine d : toDecrement)
            c.setWishlistQuantity(d.getProductID(), d.getQuantity());

        if (total > 0)
            c.addTransaction(new Transaction("ORDER", "Order placed", -total));
        report.append(String.format("Total debited: $%.2f", total));
        return report.toString();
    }

    /**
     * Receives a shipment (Stage 2, "receive a shipment").
     *
     * <p>
     * For each shipment line, the units received are first used to fill the
     * product's waitlist in the order clients joined it. Each client served is
     * charged for the units they receive with an {@code "ORDER"} transaction; a
     * client whose request is only partly filled has their waiting quantity
     * reduced and keeps their place in line. Fully served clients are removed from
     * the waitlist. Once the waitlist has been served or the shipment runs out,
     * any remaining units are added to stock. Lines for unknown products are
     * reported and skipped.
     * </p>
     *
     * @param shipmentLines the products and quantities received
     * @return a report of which clients were filled and how stock changed
     */
    public String receiveShipment(List<ShipmentLine> shipmentLines) {
        StringBuilder report = new StringBuilder();
        for (ShipmentLine line : shipmentLines) {
            String productID = line.getProductID();
            int quantityReceived = line.getQuantity();
            Product p = catalog.searchForProduct(productID);
            if (p == null) {
                report.append("Product not found: ").append(productID).append("\n");
                continue;
            }

            int remaining = quantityReceived;
            List<WaitlistItem> satisfied = new ArrayList<>(); // remove after the loop

            for (WaitlistItem entry : p.getWaitlist()) // FIFO
            {
                if (remaining <= 0)
                    break; // shipment exhausted, rest stay waiting

                int wanted = entry.getQuantity();
                int filled = Math.min(wanted, remaining);
                remaining -= filled;

                // Waiting client for what we just filled.
                Client c = clientList.searchForClient(entry.getClientID());
                if (c != null) {
                    double cost = filled * p.getPrice();
                    c.addTransaction(new Transaction("ORDER", "Waitlist filled: " + p.getName(), -cost));
                }
                report.append("Filled ").append(filled).append(" for ").append(entry.getClientID()).append("\n");

                if (filled >= wanted)
                    satisfied.add(entry); // fully fulfilled -> remove
                else
                    entry.setQuantity(wanted - filled); // partially fulfilled -> reduce, stays waiting
            }
            p.removeWaitlistEntries(satisfied); // apply removals after iterating
            p.setStock(p.getStock() + remaining);
            report.append("Added ").append(remaining).append(" to stock of ")
                    .append(p.getName()).append(". New stock: ").append(p.getStock()).append("\n"); // leftover after
                                                                                                    // waitlist -> stock
        }
        return report.toString();
    }

    /**
     * Receives a payment from a client (Stage 2, "receive a payment") and records
     * it as a positive {@code "PAYMENT"} transaction, which raises the client's
     * balance by the amount paid.
     *
     * @param clientID the id of the paying client
     * @param amount   the amount paid; must be positive
     * @return a confirmation including the client's new balance, or a message if
     *         the client is unknown or the amount is not positive
     */
    public String receivePayment(String clientID, double amount) {
        Client c = clientList.searchForClient(clientID);
        if (c == null)
            return "Client not found.";
        if (amount <= 0)
            return "Payment must be positive.";

        c.addTransaction(new Transaction("PAYMENT", "Payment received", amount)); // positive == credit
        return String.format("Payment of $%.2f recorded for %s. New balance: $%.2f",
                amount, c.getName(), c.getBalance());
    }

    /**
     * Finds a client by ID.
     *
     * @param clientID the client ID
     * @return the matching client, or {@code null} if none exists
     */
    public Client getClient(String clientID) {
        return clientList.searchForClient(clientID);
    }

    /**
     * Builds product views appropriate for a user's role.
     *
     * @param userType the role code: 0 for client, 1 for clerk, or 2 for manager
     * @return product views for the requested role
     * @throws IllegalArgumentException if {@code userType} is not a supported role
     */
    public List<ProductView> getProductListView(int userType) {
        return switch (userType) {
            case 0 -> getProductListViewForCustomer(); // Customer
            case 1 -> getProductListViewForClerk(); // Clerk
            case 2 -> getProductListViewForManager(); // Manager
            default -> throw new IllegalArgumentException("Invalid user type: " + userType);
        };
    }

    /**
     * Returns a list of product views for customers (clients).
     *
     * @return a list of product views for customers
     */
    private List<ProductView> getProductListViewForCustomer() {
        List<ProductView> views = new ArrayList<>();
        for (Product p : catalog.getAllProducts()) {
            views.add(new ProductView(p.getID(), p.getName(), p.getPrice()));
        }
        return views;
    }

    /**
     * Returns a list of product views for clerks.
     *
     * @return a list of product views for clerks
     */
    private List<ProductView> getProductListViewForClerk() {
        List<ProductView> views = new ArrayList<>();
        for (Product p : catalog.getAllProducts()) {
            views.add(new ProductView(p.getID(), p.getName(), p.getPrice(), p.getStock()));
        }
        return views;
    }

    /**
     * Returns a list of product views for managers.
     *
     * @return a list of product views for managers
     */
    private List<ProductView> getProductListViewForManager() {
        List<ProductView> views = new ArrayList<>();
        for (Product p : catalog.getAllProducts()) {
            views.add(new ProductView(p.getID(), p.getName(), p.getPrice(), p.getStock(), p.getWaitlist()));
        }
        return views;
    }
}
