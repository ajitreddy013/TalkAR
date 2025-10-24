import { Request, Response, NextFunction } from 'express';

export const errorHandler = (error: any, req: Request, res: Response, next: NextFunction) => {
  console.error('Error:', error);
  
  // Default error
  let status = error.status || 500;
  let message = 'Internal Server Error';
  
  // Handle specific error types
  if (error.name === 'ValidationError') {
    status = 400;
    message = error.message;
  } else if (error.name === 'UnauthorizedError') {
    status = 401;
    message = 'Unauthorized';
  } else if (error.name === 'ForbiddenError') {
    status = 403;
    message = 'Forbidden';
  } else if (error.name === 'NotFoundError') {
    status = 404;
    message = 'Not Found';
  } else if (error.code === 'LIMIT_FILE_SIZE') {
    status = 413;
    message = 'File too large';
  } else if (error.message) {
    message = error.message;
  }
  
  res.status(status).json({
    error: message,
    ...(process.env.NODE_ENV === 'development' && { stack: error.stack })
  });
};

