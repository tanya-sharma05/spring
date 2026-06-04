import { useEffect, useState } from "react";
import { useNavigate, useParams } from "react-router-dom";
import axios from "../axios";

const UpdateProduct = () => {
  const { id } = useParams();
  const navigate = useNavigate();

  const [product, setProduct] = useState({
    name: "",
    brand: "",
    desc: "",
    price: "",
    category: "",
    stockQuantity: "",
    releaseDate: "",
    available: true,
  });

  const [image, setImage] = useState(null);

  // Fetch old product data
  useEffect(() => {
    const fetchProduct = async () => {
      try {
        const response = await axios.get(
          `http://localhost:8080/api/product/${id}`
        );

        setProduct(response.data);

      } catch (error) {
        console.log("Error fetching product", error);
      }
    };

    fetchProduct();

  }, [id]);


  // Update form values
  const handleChange = (e) => {
    setProduct({
      ...product,
      [e.target.name]: e.target.value,
    });
  };


  // Update image
  const handleImageChange = (e) => {
    setImage(e.target.files[0]);
  };


  // Submit update
  const handleSubmit = async (e) => {
    e.preventDefault();

    try {

      const formData = new FormData();

      formData.append(
        "product",
        new Blob(
          [JSON.stringify(product)],
          { type: "application/json" }
        )
      );

      if (image) {
        formData.append("imageFile", image);
      }


      await axios.put(
        `http://localhost:8080/api/product/${id}`,
        formData,
        {
          headers: {
            "Content-Type": "multipart/form-data",
          },
        }
      );


      alert("Product Updated Successfully");

      navigate(`/product/${id}`);

    } catch (error) {
      console.log("Error updating product", error);
    }
  };


  return (
    <div className="container">

      <h2>Update Product</h2>

      <form onSubmit={handleSubmit}>

        <input
          type="text"
          name="name"
          value={product.name}
          onChange={handleChange}
          placeholder="Product Name"
        />

        <br />

        <input
          type="text"
          name="brand"
          value={product.brand}
          onChange={handleChange}
          placeholder="Brand"
        />

        <br />

        <input
          type="text"
          name="desc"
          value={product.desc}
          onChange={handleChange}
          placeholder="Description"
        />

        <br />

        <input
          type="number"
          name="price"
          value={product.price}
          onChange={handleChange}
          placeholder="Price"
        />

        <br />

        <input
          type="text"
          name="category"
          value={product.category}
          onChange={handleChange}
          placeholder="Category"
        />

        <br />

        <input
          type="number"
          name="stockQuantity"
          value={product.stockQuantity}
          onChange={handleChange}
          placeholder="Stock Quantity"
        />

        <br />

        <input
          type="date"
          name="releaseDate"
          value={product.releaseDate}
          onChange={handleChange}
        />

        <br />

        <label>
          Available:
          <input
            type="checkbox"
            name="available"
            checked={product.available}
            onChange={(e) =>
              setProduct({
                ...product,
                available: e.target.checked,
              })
            }
          />
        </label>

        <br />

        <input
          type="file"
          onChange={handleImageChange}
        />

        <br /><br />

        <button type="submit">
          Update Product
        </button>

      </form>

    </div>
  );
};

export default UpdateProduct;