import { Request, Response, NextFunction } from "express";
import Joi from "joi";

export const validateImageUpload = (
  req: Request,
  res: Response,
  next: NextFunction
) => {
  const schema = Joi.object({
    name: Joi.string().required().min(1).max(100),
    description: Joi.string().optional().max(500),
  });

  const { error } = schema.validate(req.body);
  if (error) {
    return res.status(400).json({ error: error.details[0].message });
  }

  if (!req.file) {
    return res.status(400).json({ error: "Image file is required" });
  }

  return next();
};

export const validateSyncRequest = (
  req: Request,
  res: Response,
  next: NextFunction
) => {
  const schema = Joi.object({
    text: Joi.string().required().min(1).max(1000),
    language: Joi.string().required().length(2),
    voiceId: Joi.string().optional(),
  });

  const { error } = schema.validate(req.body);
  if (error) {
    return res.status(400).json({ error: error.details[0].message });
  }

  return next();
};

export const validateDialogue = (
  req: Request,
  res: Response,
  next: NextFunction
) => {
  const schema = Joi.object({
    text: Joi.string().required().min(1).max(1000),
    language: Joi.string().required().length(2),
    voiceId: Joi.string().optional(),
    isDefault: Joi.boolean().optional(),
  });

  const { error } = schema.validate(req.body);
  if (error) {
    return res.status(400).json({ error: error.details[0].message });
  }

  return next();
};

export const validateAuthRequest = (
  req: Request,
  res: Response,
  next: NextFunction
) => {
  const schema = Joi.object({
    email: Joi.string().email().required(),
    password: Joi.string().min(6).required(),
    role: Joi.string().valid("admin", "user").optional(),
  });

  const { error } = schema.validate(req.body);
  if (error) {
    return res.status(400).json({ error: error.details[0].message });
  }

  return next();
};

export const validateChangePassword = (
  req: Request,
  res: Response,
  next: NextFunction
) => {
  const schema = Joi.object({
    currentPassword: Joi.string().required(),
    newPassword: Joi.string().min(6).required(),
  });

  const { error } = schema.validate(req.body);
  if (error) {
    return res.status(400).json({ error: error.details[0].message });
  }

  return next();
};
