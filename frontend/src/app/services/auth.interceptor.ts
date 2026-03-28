// Service containing business logic for auth.interceptor
import { HttpInterceptorFn, HttpErrorResponse } from '@angular/common/http';
import { inject } from '@angular/core';
import { catchError, throwError } from 'rxjs';
import { AuthService } from './auth.service';
export const authInterceptor: HttpInterceptorFn = (req, next) => {
  const auth = inject(AuthService);
  const token = auth.getToken();
  let requestToForward = req;
  if (token) {
    requestToForward = req.clone({
      setHeaders: { Authorization: `Bearer ${token}` }
    });
  }
  return next(requestToForward).pipe(
    catchError((error: HttpErrorResponse) => {
      let errorMessage = 'An unknown error occurred!';
      if (error.error instanceof ErrorEvent) {
        errorMessage = `Network Error: ${error.error.message}`;
      } else {
        errorMessage = `Server Error [${error.status}]: ${error.message}`;
        if (error.status === 401) {
          console.warn('Unauthorized request - session may have expired.');
        }
      }
      console.error('🌐 HTTP Global Error:', errorMessage, error);
      return throwError(() => error);
    })
  );
};