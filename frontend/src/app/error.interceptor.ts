import { HttpErrorResponse, HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { MatSnackBar } from '@angular/material/snack-bar';
import { catchError, throwError } from 'rxjs';

/**
 * Shows every failed API call in a snackbar, using the backend's ProblemDetail "detail"
 * when there is one. The error is re-thrown so components can still react to it.
 */
export const errorInterceptor: HttpInterceptorFn = (req, next) => {
  const snackBar = inject(MatSnackBar);
  return next(req).pipe(
    catchError((error: HttpErrorResponse) => {
      const message =
        error.status === 0
          ? 'Cannot reach the server.'
          : (error.error?.detail ?? `Request failed (${error.status}).`);
      snackBar.open(message, 'Close', { duration: 6000 });
      return throwError(() => error);
    }),
  );
};
