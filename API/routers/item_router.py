from typing import List

from PIL import Image
from fastapi import APIRouter, Depends, HTTPException, File

from backend import SearchImage
from database.dal import DAL, get_dal
import database.schemas as schemas
import database.models as models
from login.login_utils import get_current_active_user
from fastapi.responses import HTMLResponse
import io

item_router = APIRouter()
search = SearchImage()
search.load_index()


@item_router.post("/items/", response_model=schemas.Item)
async def create_item(item: schemas.ItemBase, db: DAL = Depends(get_dal)):
    return await db.create_user_item(item=item)


@item_router.get("/items/", response_model=List[schemas.Item])
async def get_all_items(skip: int = 0, limit: int = 100, db: DAL = Depends(get_dal)) -> List[models.Items]:
    items = await db.get_items(skip=skip, limit=limit)
    return items


@item_router.post("/items/upload/", response_model=schemas.Item)
async def get_item_data_from_image(file=File(...), db: DAL = Depends(get_dal)):
    # items = await db.get_items()
    # if file.content_type:
    if file.content_type != "image/jpeg":
        return HTTPException(400, detail='Invalid image_id')
    content = await file.read()
    image = Image.open(io.BytesIO(content))
    image_id = search.get_image_index(image)
    item = await db.get_item_by_image_id(image_id)
    print(image_id, item)
    if item is None:
        raise HTTPException(status_code=404, detail="Item not found")
    return item


@item_router.get("/items/upload/")
async def upload():
    content = """
<body>
<form action="/items/upload/" enctype="multipart/form-data" method="post">
<input name="file" type="file" multiple>
<input type="submit">
</form>
</body>
    """
    return HTMLResponse(content=content)
