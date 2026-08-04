package com.commercehub.messaging;

public final class MessagingTopology {

    public static final String EXCHANGE_EVENTS = "commercehub.events";

    public static final String ROUTING_ORDER_CREATED = "order.created";
    public static final String ROUTING_ORDER_CANCELLED = "order.cancelled";
    public static final String ROUTING_STOCK_RESERVED = "stock.reserved";
    public static final String ROUTING_STOCK_RELEASED = "stock.released";

    public static final String QUEUE_INVENTORY_ORDER_CREATED = "inventory.order-created";
    public static final String QUEUE_INVENTORY_ORDER_CANCELLED = "inventory.order-cancelled";
    public static final String QUEUE_ORDER_STOCK_RESERVED = "order.stock-reserved";
    public static final String QUEUE_NOTIFICATION_ORDER_EVENTS = "notification.order-events";
    public static final String QUEUE_NOTIFICATION_STOCK_EVENTS = "notification.stock-events";

    public static final String BINDING_ORDER_ALL = "order.#";
    public static final String BINDING_STOCK_ALL = "stock.#";

    private MessagingTopology() {
    }
}
