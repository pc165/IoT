from typing import List

from fastapi import APIRouter, Depends
from fastapi.encoders import jsonable_encoder
from fastapi.responses import HTMLResponse
from json2html import json2html
from pydantic import parse_obj_as

from database.dal import DAL, get_dal
from database import schemas

data_router = APIRouter()


@data_router.get("/")
async def get_latest_orders(skip=0, limit=10, db: DAL = Depends(get_dal)):
    orders = await db.get_all_orders(skip=skip, limit=limit, desc=True)
    orders: List[schemas.Order] = parse_obj_as(List[schemas.Order], orders)
    for i in orders:
        for j in i.order_details:
            i.total += j.items.price
    json = jsonable_encoder(orders)
    table = json2html.convert(json=json)
    # TODO buttons to handle params http://127.0.0.1:8000/?skip=0&limit=30
    html = f"""
    <!DOCTYPE html>
    <html>
        <head>
        </head>
        <body>
        {table}
        </body>
    </html>
    """
    return HTMLResponse(html)
