import java.util.ArrayList;
import java.util.List;

/**
 * The collection of every {@link Client} registered with the warehouse.
 *
 * <p>
 * The client list owns the client collection: the {@link Warehouse} adds,
 * looks up, and reaches a client's wishlist only through these methods and
 * never
 * handles the underlying list directly. Each client id may appear in the list
 * at
 * most once.
 * </p>
 */
public class ClientList {
    /** All registered clients, in the order they were added. */
    private final List<Client> clients = new ArrayList<>();

    /**
     * Adds a client to the list, unless a client with the same id is already
     * present.
     *
     * @param client the client to add
     * @return {@code true} if the client was added; {@code false} if a client
     *         with the same id already exists
     */
    public boolean addClient(Client client) {
        if (searchForClient(client.getID()) != null)
            return false; // uniqueness
        clients.add(client);
        return true;
    }

    /**
     * Finds the client with the given id.
     *
     * @param clientID the id to look for
     * @return the matching client, or {@code null} if there is none
     */
    public Client searchForClient(String clientID) {
        for (Client c : clients) {
            if (c.getID().equals(clientID))
                return c;
        }
        return null;
    }

    /**
     * Adds one unit of a product to a client's wishlist.
     *
     * @param clientID  the id of the client
     * @param productID the id of the product to add
     * @return {@code true} if the entry was added; {@code false} if no client has
     *         the given id
     * @see #addClientWishlistEntry(String, String, int)
     */
    public boolean addClientWishlistEntry(String clientID, String productID) {
        return addClientWishlistEntry(clientID, productID, 1);
    }

    /**
     * Adds a quantity of a product to a client's wishlist. If the product is
     * already on that wishlist, the quantity is added to the existing entry.
     *
     * <p>
     * This method does not check that the product exists; the
     * {@link Warehouse} validates the product id before calling it.
     * </p>
     *
     * @param clientID  the id of the client
     * @param productID the id of the product to add
     * @param quantity  the number of units to add
     * @return {@code true} if the entry was added; {@code false} if no client has
     *         the given id
     */
    public boolean addClientWishlistEntry(String clientID, String productID, int quantity) {
        Client c = searchForClient(clientID);
        if (c == null)
            return false;
        return c.addWishlistEntry(productID, quantity);
    }

    /**
     * Removes a product from a client's wishlist.
     *
     * @param clientID  the id of the client
     * @param productID the id of the product to remove
     * @return {@code true} if the entry was removed; {@code false} if the client
     *         is unknown or the product is not on that wishlist
     */
    public boolean removeClientWishlistEntry(String clientID, String productID) {
        Client c = searchForClient(clientID);
        if (c == null || c.getWishlistQuantity(productID) == 0)
            return false;
        c.removeWishlistEntry(productID);
        return true;
    }

    /**
     * Changes the quantity of a product already on a client's wishlist.
     *
     * @param clientID  the id of the client
     * @param productID the id of the product
     * @param quantity  the new quantity
     * @return {@code true} if the entry was updated; {@code false} if the client
     *         is unknown or the product is not on that wishlist
     */
    public boolean setClientWishlistQuantity(String clientID, String productID, int quantity) {
        Client c = searchForClient(clientID);
        if (c == null || c.getWishlistQuantity(productID) == 0)
            return false;
        c.setWishlistQuantity(productID, quantity);
        return true;
    }

    /**
     * Returns every registered client, in the order they were added.
     *
     * <p>
     * The returned list is a copy, so adding to or removing from it does not
     * change the client list. The clients themselves are shared.
     * </p>
     *
     * @return a new list containing all clients
     */
    public List<Client> getAllClients() {
        return new ArrayList<>(clients);
    }

    /**
     * Returns the wishlist of the client with the given id.
     *
     * <p>
     * Unlike {@link #getAllClients()}, this returns the client's
     * <em>actual</em> wishlist rather than a copy, so changes made to the
     * returned list change the client's wishlist.
     * </p>
     *
     * @param clientID the id of the client
     * @return the client's wishlist entries, or {@code null} if no client has the
     *         given id
     */
    public List<WishlistItem> getClientWishlist(String clientID) {
        Client c = searchForClient(clientID);
        if (c == null)
            return null;
        return c.getWishListItems();
    }
}
