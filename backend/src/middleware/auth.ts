import { Request, Response, NextFunction } from "express";
import jwt from "jsonwebtoken";

interface AuthRequest extends Request {
  user?: any;
}

export const authenticateAdmin = (
  req: AuthRequest,
  res: Response,
  next: NextFunction
) => {
  try {
    const token = req.header("Authorization")?.replace("Bearer ", "");

    if (!token) {
      return res
        .status(401)
        .json({ error: "Access denied. No token provided." });
    }

    const decoded = jwt.verify(
      token,
      process.env.JWT_SECRET || "fallback-secret"
    );
    req.user = decoded;

    // Check if user has admin role
    if (req.user.role !== "admin") {
      return res
        .status(403)
        .json({ error: "Access denied. Admin role required." });
    }

    return next();
  } catch (error) {
    return res.status(401).json({ error: "Invalid token." });
  }
};

export const authenticateUser = (
  req: AuthRequest,
  res: Response,
  next: NextFunction
) => {
  try {
    const token = req.header("Authorization")?.replace("Bearer ", "");

    if (!token) {
      return res
        .status(401)
        .json({ error: "Access denied. No token provided." });
    }

    const decoded = jwt.verify(
      token,
      process.env.JWT_SECRET || "fallback-secret"
    );
    req.user = decoded;

    return next();
  } catch (error) {
    return res.status(401).json({ error: "Invalid token." });
  }
};
