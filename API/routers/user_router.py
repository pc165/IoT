from typing import List
from fastapi import APIRouter, Depends, HTTPException
from database.dal import DAL, get_dal
import database.schemas as schemas
import database.models as models
from login.login_utils import get_current_active_user

user_router = APIRouter()


@user_router.post("/users/", response_model=schemas.User)
async def create_user(user: schemas.UserCreate, db: DAL = Depends(get_dal)):
    db_user = await db.get_user_by_email(email=user.email)
    if db_user:
        raise HTTPException(status_code=400, detail="Email already registered")
    return await db.create_user(user=user)


@user_router.get("/users/", response_model=List[schemas.User])
async def get_all_users(skip: int = 0, limit: int = 100, db: DAL = Depends(get_dal)) -> List[models.Users]:
    return await db.get_all_users(skip=skip, limit=limit)


@user_router.get("/users/{user_id}", response_model=schemas.User)
async def get_user(user_id: int, db: DAL = Depends(get_dal)) -> models.Users:
    db_user = await db.get_user_by_id(user_id=user_id)
    if db_user is None:
        raise HTTPException(status_code=404, detail="User not found")
    return db_user


@user_router.post("/users/{user_id}/orders", response_model=schemas.Order)
async def create_order(user_id: int, order: schemas.OrderBase, db: DAL = Depends(get_dal)):
    db_user = await db.get_user_by_id(user_id=user_id)
    if db_user is None:
        raise HTTPException(status_code=404, detail="User not found")
    return await db.create_order(user_id=user_id, order=order)


@user_router.get("/users/{user_id}/orders", response_model=List[schemas.Order])
async def get_orders_from_user(user_id: int, skip: int = 0, limit: int = 100, db: DAL = Depends(get_dal)) -> List[
    schemas.Order]:
    db_user = await db.get_user_by_id(user_id=user_id)
    if db_user is None:
        raise HTTPException(status_code=404, detail="User not found")
    return await db.get_all_orders_from_user(user_id=user_id, skip=skip, limit=limit)


@user_router.get("/orders", response_model=List[schemas.Order])
async def get_all_orders(skip: int = 0, limit: int = 30, db: DAL = Depends(get_dal)) -> List[schemas.Order]:
    return await db.get_all_orders(skip=skip, limit=limit)


@user_router.get("/users/me/", response_model=schemas.User)
async def get_user_me(current_user: schemas.User = Depends(get_current_active_user)):
    return current_user


@user_router.get("/users/me/orders/", response_model=List[schemas.Order])
async def get_my_orders(skip: int = 0, limit: int = 100,
                        db: DAL = Depends(get_dal),
                        current_user: schemas.User = Depends(get_current_active_user)) -> List[schemas.Order]:
    return await db.get_all_orders_from_user(user_id=current_user.id, skip=skip, limit=limit)


@user_router.post("/users/me/orders/", response_model=schemas.Order)
async def create_order_for_me(order: schemas.OrderBase, db: DAL = Depends(get_dal),
                              current_user: schemas.User = Depends(get_current_active_user)) -> List[schemas.Order]:
    return await db.create_order(user_id=current_user.id, order=order)
