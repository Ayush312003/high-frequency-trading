import requests
import random
import time
import uuid

# Configuration
API_URL = "http://localhost:8080/api/orders"
USERS = ["Alice", "Bob", "Charlie", "David", "Eve", "HFT_Bot_1", "HFT_Bot_2"]

def generate_order():
    """Generates a random BUY or SELL order around the $50,000 price point"""

    # Random price between $49,000 and $51,000
    base_price = 50000
    variance = random.randint(-1000, 1000)
    price = base_price + variance

    # Random quantity between 0.1 and 2.0 BTC
    quantity = round(random.uniform(0.1, 2.0), 4)

    # Pick a random user
    user_id = random.choice(USERS)

    # Determine side
    side = random.choice(["BUY", "SELL"])

    return {
        "userId": user_id,
        "type": side,
        "price": price,
        "quantity": quantity
    }

def run_market_maker():
    print(f"🚀 Starting High-Frequency Market Maker on {API_URL}")
    print("Press CTRL+C to stop...")

    order_count = 0
    try:
        while True:
            order = generate_order()
            try:
                response = requests.post(API_URL, json=order)
                if response.status_code == 200:
                    print(f"[{order_count}] Sent {order['type']} {order['quantity']} BTC @ ${order['price']} ({order['userId']})")
                else:
                    print(f"❌ Error: {response.text}")
            except Exception as e:
                print(f"❌ Connection Error: {e}")

            order_count += 1
            # Sleep for 100ms to simulate 10 orders/second (Adjust as needed)
            time.sleep(0.00001)

    except KeyboardInterrupt:
        print("\n🛑 Market Maker stopped.")

if __name__ == "__main__":
    run_market_maker()