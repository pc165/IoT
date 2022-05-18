import asyncio
from datetime import datetime, timedelta
from random import randint
import pandas as pd
from database import models
from database.config import async_session, Base
from database.config import engine
from zipfile import ZipFile
import os


# Load dataset
def preDot(text):
    return text.rsplit(".", 1)[0]


dataset = {}

for dirname, _, filenames in os.walk("./dataset"):
    for filename in filenames:
        print(os.path.join(dirname, filename))
        with ZipFile(os.path.join(dirname, filename), "r") as zipf:
            unzipped_fn = preDot(filename)
            with zipf.open(unzipped_fn + ".csv") as f:
                dataset[preDot(unzipped_fn)] = pd.read_csv(f)

# process data
orders: pd.DataFrame = dataset["orders"][dataset["orders"]["eval_set"] == "train"].drop("eval_set", axis=1)[:5000]
products: pd.DataFrame = dataset["products"].drop(["aisle_id", "department_id"], axis=1)
order_products: pd.DataFrame = dataset["order_products"].drop("reordered", axis=1)

# assing each order to a random user
orders["user_id"] = orders["user_id"].apply(lambda x: randint(1, 500))
users = pd.DataFrame = orders["user_id"].drop_duplicates()


async def update_db(data):
    async with async_session() as db:
        async with db.begin():
            for i in data.values:
                db.add(i)
            await db.flush()


def f(order_row):
    # random date
    date = datetime(2020, randint(1, 12), randint(1, 29), int(order_row["order_hour_of_day"]), randint(0, 59),
                    randint(0, 59), randint(0, 59))
    details = order_products[order_products["order_id"] == order_row["order_id"]]
    details = details.apply(lambda row: models.OrderDetails(order_id=int(row["order_id"]),
                                                            item_id=int(row["product_id"]),
                                                            quantity=randint(1, 10)), axis=1)
    monday_date = date - timedelta(days=date.weekday())
    order_date = monday_date + timedelta(days=order_row["order_dow"])
    a = models.Orders(id=int(order_row["order_id"]), user_id=int(order_row["user_id"]), datetime=order_date,
                      order_details=details.values.tolist())
    return a


def f2(user_id):
    orders_user = orders[orders["user_id"] == user_id]
    # get items for each order
    orders_db = orders_user.apply(f, axis=1)
    a = models.Users(id=int(user_id), username=int(user_id), email=int(user_id), orders=orders_db.values.tolist())
    return a


async def main():
    async with engine.begin() as conn:
        await conn.run_sync(Base.metadata.drop_all)
        await conn.run_sync(Base.metadata.create_all)

    # get orders for each user
    users_db = users.apply(f2)
    await update_db(users_db)

    items_db = products.apply(
            lambda row: models.Items(id=int(row["product_id"]),
                                     name=row["product_name"], price=0), axis=1)
    await update_db(items_db)


asyncio.run(main())
