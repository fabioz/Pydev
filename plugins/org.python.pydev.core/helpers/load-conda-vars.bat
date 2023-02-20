@echo off

@setlocal

@set "CONDA_PREFIX=%__PYDEV_CONDA_PREFIX__%"
@set "CONDA_ENV_PATH=%CONDA_PREFIX%"
@set "CONDA_PATH_BACKUP=%PATH%"
@set "PATH=%CONDA_PREFIX%;%CONDA_PREFIX%\Scripts;%CONDA_PREFIX%\Library\bin;%PATH%"

@REM activate conda and get the environment
@call %__PYDEV_CONDA_ACTIVATION__%
@call activate %__PYDEV_CONDA_DEFAULT_ENV__%

@REM Print existing environment variables with the loaded env.
SET

@endlocal