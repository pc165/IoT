from typing import List
from sqlalchemy import update
from sqlalchemy.future import select
from sqlalchemy.orm import Session
from . import models, schemas
from database.config import async_session
from login.password_utils import get_password_hash


class DAL:
    def __init__(self, db_session: Session):
        self.db = db_session

    """
    User DB operations
    """

    async def get_user_by_id(self, user_id: int):
        query = select(models.Users).where(models.Users.id == user_id)
        response = await self.db.execute(query)
        response = response.scalars().first()
        return response

    async def get_user_by_username(self, username: str):
        query = select(models.Users).where(models.Users.username == username)
        response = await self.db.execute(query)
        response = response.scalars().first()
        return response

    async def get_user_by_email(self, email: str):
        query = select(models.Users).where(models.Users.email == email)
        response = await self.db.execute(query)
        return response.first()

    async def get_all_users(self, skip: int = 0, limit: int = 100):
        query = select(models.Users).offset(skip).limit(limit)
        response = await self.db.execute(query)
        response = response.scalars().all()
        return response

    async def update_user_password(self, user: schemas.UserCreate):
        hashed_password = get_password_hash(user.password)
        query = update(models.Users).where(models.Users.email == user.email).values(hashed_password=hashed_password)
        query.execution_options(synchronize_session="fetch")
        await self.db.execute(query)
        return None

    async def create_user(self, user: schemas.UserCreate):
        hashed_password = get_password_hash(user.password)
        new_user = models.Users(hashed_password=hashed_password, **user.dict(exclude={'password'}))
        self.db.add(new_user)
        await self.db.flush()
        await self.db.refresh(new_user)
        return new_user

    """
    Orders DB operations
    """

    async def create_order(self, user_id: int, order: schemas.OrderBase):
        new_order = models.Orders(user_id=user_id)
        new_order.order_details = [models.OrderDetails(**i.dict()) for i in order.order_details]
        self.db.add(new_order)
        await self.db.flush()
        await self.db.refresh(new_order)
        return new_order

    async def get_all_orders_from_user(self, user_id: int, skip: int = 0, limit: int = 100):
        query = select(models.Orders) \
            .where(models.Orders.user_id == user_id) \
            .order_by(models.Orders.id).offset(skip).limit(limit)
        response = await self.db.execute(query)
        response = response.scalars().fetchall()
        return response

    async def get_all_orders(self, skip: int = 0, limit: int = 100, desc: bool = False):
        query = select(models.Orders) \
            .order_by(models.Orders.id.desc() if desc else models.Orders.id).offset(skip).limit(limit)

        response = await self.db.execute(query)
        response = response.scalars().all()
        return response

    """
    Item DB operations
    """

    async def get_items(self, skip: int = 0, limit: int = 100):
        query = select(models.Items).order_by(models.Items.id).offset(skip).limit(limit)
        response = await self.db.execute(query)
        response = response.scalars().fetchall()
        return response

    async def get_item_by_image_id(self, image_id: int):
        query = select(models.Items).where(models.Items.image_id == image_id)
        response = await self.db.execute(query)
        response = response.scalars().first()
        return response

    async def create_user_item(self, item: schemas.ItemBase):
        new_item = models.Items(**item.dict())
        self.db.add(new_item)
        await self.db.flush()
        await self.db.refresh(new_item)
        return new_item


async def get_dal():
    async with async_session() as session:
        async with session.begin():
            yield DAL(session)
