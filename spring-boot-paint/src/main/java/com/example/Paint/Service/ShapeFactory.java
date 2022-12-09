package com.example.Paint.Service;

import com.example.Paint.input.ShapeInput;
import com.example.Paint.model.*;

public class ShapeFactory {

    public static Shape getShape(ShapeInput shapeInput) {
        String type = shapeInput.type;

        System.out.println("HERE" + shapeInput.x);

        if (type.equalsIgnoreCase("line")) {
            return new Line(shapeInput.endX, shapeInput.endY, shapeInput.stroke, shapeInput.fill,
                    shapeInput.id, shapeInput.x, shapeInput.y, shapeInput.strokeWidth, shapeInput.rotation);

        } else if (type.equalsIgnoreCase("circle")) {
            return new Circle(shapeInput.radius, shapeInput.stroke, shapeInput.fill,
                    shapeInput.id, shapeInput.x, shapeInput.y, shapeInput.strokeWidth, shapeInput.rotation);

        } else if (type.equalsIgnoreCase("ellipse")) {
            return new Ellipse(shapeInput.width, shapeInput.height, shapeInput.stroke, shapeInput.fill,
                    shapeInput.id, shapeInput.x, shapeInput.y, shapeInput.strokeWidth, shapeInput.rotation);

        } else if (type.equalsIgnoreCase("square")) {
            return new Square(shapeInput.length, shapeInput.stroke, shapeInput.fill, shapeInput.id,
                    shapeInput.x, shapeInput.y, shapeInput.strokeWidth, shapeInput.rotation, shapeInput.cornerRadius);

        } else if (type.equalsIgnoreCase("rectangle")) {
            return new Rectangle(shapeInput.length, shapeInput.width, shapeInput.stroke, shapeInput.fill, shapeInput.id,
                    shapeInput.x, shapeInput.y, shapeInput.strokeWidth, shapeInput.rotation, shapeInput.cornerRadius);

        } else if (type.equalsIgnoreCase("triangle")) {
            return new Triangle(shapeInput.width, shapeInput.height, shapeInput.stroke, shapeInput.fill, shapeInput.id,
                    shapeInput.x, shapeInput.y, shapeInput.strokeWidth, shapeInput.rotation, shapeInput.cornerRadius);
        }

        return null;
    }
}
