# https://github.com/TechyNilesh/DeepImageSearch/blob/main/DeepImageSearch/DeepImageSearch.py
import os
import pandas as pd
import matplotlib.pyplot as plt
from PIL import Image
import numpy as np
from annoy import AnnoyIndex
from tqdm import tqdm
from tensorflow.keras.preprocessing import image
from tensorflow.keras.applications.vgg16 import VGG16, preprocess_input
from tensorflow.keras.models import Model
import tensorflow as tf

tf.compat.v1.logging.set_verbosity(tf.compat.v1.logging.ERROR)


class SearchImage:
    data_path = 'metadata/image_data_features.pkl'
    feature_path = 'metadata/image_features_vectors.ann'

    def __init__(self):
        self.index = None
        self.data = None
        base_model = VGG16(weights='imagenet')
        self.model = Model(inputs=base_model.input, outputs=base_model.get_layer('fc1').output)

    def create_index(self, folder):
        os.makedirs('../metadata')
        data = pd.DataFrame()
        data['images_paths'] = [os.path.join(folder, path) for path in os.listdir(folder)]
        data['features'] = [self.extract(img=Image.open(img_path)) for img_path in data['images_paths']]
        data = data.dropna().reset_index(drop=True)
        index = AnnoyIndex(len(data['features'][0]), 'euclidean')
        for i, v in tqdm(zip(data.index, data['features'])):
            index.add_item(i, v)
        index.build(100)
        data.to_pickle(self.data_path)
        index.save(self.feature_path)
        self.index = index
        self.data = data

    def load_index(self):
        self.data = pd.read_pickle(self.data_path)
        self.index = AnnoyIndex(len(self.data['features'][0]), 'euclidean')
        self.index.load(self.feature_path)

    def extract(self, img):
        img = img.resize((224, 224))
        img = img.convert('RGB')
        x = image.img_to_array(img)
        x = np.expand_dims(x, axis=0)
        x = preprocess_input(x)
        feature = self.model.predict(x)[0]
        return feature / np.linalg.norm(feature)

    def get_similar_images(self, img, n: int = 1):
        query_vector = self.extract(img)
        index_list = self.index.get_nns_by_vector(query_vector, n)
        img_dict = dict(zip(index_list, self.data.iloc[index_list]['images_paths'].to_list()))
        return img_dict

    def get_image_index(self, img):
        query_vector = self.extract(img)
        index_list = self.index.get_nns_by_vector(query_vector, 1)
        return index_list[0]

    def plot_similar_images(self, img):
        axes = []
        img_list = list(self.get_similar_images(img=img, n=16).values())
        fig = plt.figure(figsize=(20, 15))
        for a in range(4 * 4):
            axes.append(fig.add_subplot(4, 4, a + 1))
            plt.imshow(Image.open(img_list[a]))
        plt.show()


if __name__ == "__main__":
    s = SearchImage()
    s.load_index()
    # s.create_index('images')
    # similar = s.get_similar_images(img=Image.open('images/57246.jpg'), n=2)
    i = s.get_image_index(img=Image.open('../images/1.jpg'))
    print(i)
